package nl.cge.lang.typescript.runtime;

import nl.cge.lang.typescript.TypeScriptTypeSystemGen;
import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.functions.FunctionDispatchNode;
import nl.cge.lang.typescript.nodes.exprs.functions.FunctionDispatchNodeGen;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * The object that represents a function in TypeScript.
 * Very similar to the class with the same name from part 16,
 * the only difference is the added {@link #materializedFrame}
 * field, which is not-{@code null} for functions that are closures.
 */
@ExportLibrary(InteropLibrary.class)
public final class FunctionObject extends JavaScriptObject {
    public final CallTarget callTarget;
    public final int argumentCount;
    public final MaterializedFrame materializedFrame;

    private final FunctionDispatchNode functionDispatchNode;

    public FunctionObject(Shape shape, ClassPrototypeObject functionPrototype, CallTarget callTarget, int argumentCount) {
        this(shape, functionPrototype, callTarget, argumentCount, null);
    }

    public FunctionObject(Shape shape, ClassPrototypeObject functionPrototype, CallTarget callTarget, int argumentCount, MaterializedFrame materializedFrame) {
        super(shape, functionPrototype);

        this.callTarget = callTarget;
        this.argumentCount = argumentCount;
        this.materializedFrame = materializedFrame;
        this.functionDispatchNode = FunctionDispatchNodeGen.create();
    }

    /**
     * Returns the string representation of a given function.
     * In JavaScript, this returns the actual code of a given function (!).
     * We'll simplify in TypeScript, and just return the string {@code "[object Function]"}.
     */
    @Override
    public String toString() {
        return "[object Function]";
    }

    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) {
        // we have to make sure the given arguments are valid TypeScript values,
        // as this class can be invoked from other languages, like Java
        for (Object argument : arguments) {
            if (!this.isTypeScriptValue(argument)) {
                throw new TypeScriptException("'" + argument + "' is not an TypeScript value");
            }
        }
        // for GraalVM polyglot calls, we never fill the receiver
        return this.functionDispatchNode.executeDispatch(this, arguments, Undefined.INSTANCE);
    }

    private boolean isTypeScriptValue(Object argument) {
        // as of this chapter, the only available types in TypeScript are
        // numbers (ints and doubles), booleans, 'undefined', functions, and strings
        return TypeScriptTypeSystemGen.isImplicitDouble(argument) ||
                TypeScriptTypeSystemGen.isBoolean(argument) ||
                argument == Undefined.INSTANCE ||
                argument instanceof TruffleString ||
                argument instanceof String ||
                argument instanceof JavaScriptObject;
    }
}

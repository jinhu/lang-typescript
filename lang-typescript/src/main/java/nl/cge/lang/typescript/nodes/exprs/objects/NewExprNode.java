package nl.cge.lang.typescript.nodes.exprs.objects;

import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.exprs.functions.FunctionDispatchNode;
import nl.cge.lang.typescript.nodes.exprs.functions.FunctionDispatchNodeGen;
import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import nl.cge.lang.typescript.runtime.FunctionObject;
import nl.cge.lang.typescript.runtime.JavaScriptObject;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import java.util.List;

/**
 * The Node for handling {@code new} expressions.
 * Identical to the class with the same name from part 16.
 */
public abstract class NewExprNode extends TypeScriptExprNode {
    @Child
    @Executed
    protected TypeScriptExprNode constructorExpr;

    @Children
    private final TypeScriptExprNode[] args;

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private FunctionDispatchNode constructorDispatchNode;

    protected NewExprNode(TypeScriptExprNode constructorExpr, List<TypeScriptExprNode> args) {
        this.constructorExpr = constructorExpr;
        this.args = args.toArray(TypeScriptExprNode[]::new);
        this.constructorDispatchNode = FunctionDispatchNodeGen.create();
    }

    /**
     * The specialization for when the constructor expression evaluates to an
     * {@link ClassPrototypeObject}.
     */
    @Specialization(limit = "2")
    protected Object instantiateObject(VirtualFrame frame, ClassPrototypeObject classPrototypeObject,
            @CachedLibrary("classPrototypeObject") InteropLibrary interopPrototypeLibrary) {
        var object = new JavaScriptObject(this.currentLanguageContext().shapesAndPrototypes.rootShape, classPrototypeObject);
        Object constructor = null;
        try {
            constructor = interopPrototypeLibrary.readMember(classPrototypeObject, "constructor");
        } catch (UnknownIdentifierException e) {
            // fall through to below
        } catch (UnsupportedMessageException e) {
            throw new TypeScriptException(this, e.getMessage());
        }
        if (constructor instanceof FunctionObject) {
            // instanceof always returns 'false' for 'null'
            Object[] args = this.executeArguments(frame);
            var boundConstructor = (FunctionObject) constructor;
            this.constructorDispatchNode.executeDispatch(boundConstructor, args, object);
        } else {
            this.consumeArguments(frame);
        }
        return object;
    }

    /**
     * The specialization for when the constructor expression evaluates to something other than
     * {@link ClassPrototypeObject}.
     */
    @Fallback
    protected Object instantiateNonConstructor(VirtualFrame frame, Object object) {
        this.consumeArguments(frame);
        throw new TypeScriptException("'" + object + "' is not a constructor");
    }

    /**
     * Even though we don't use the arguments for anything in this part of the series,
     * we need to evaluate them, as they can have side effects
     * (like assignment).
     */
    @ExplodeLoop
    private void consumeArguments(VirtualFrame frame) {
        for (int i = 0; i < this.args.length; i++) {
            this.args[i].executeGeneric(frame);
        }
    }

    @ExplodeLoop
    private Object[] executeArguments(VirtualFrame frame) {
        var args = new Object[this.args.length];
        for (int i = 0; i < this.args.length; i++) {
            args[i] = this.args[i].executeGeneric(frame);
        }
        return args;
    }
}

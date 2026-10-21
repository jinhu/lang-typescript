package nl.cge.lang.typescript.nodes.exprs;

import nl.cge.lang.typescript.TypeScriptTypeSystem;
import nl.cge.lang.typescript.TypeScriptTypeSystemGen;
import nl.cge.lang.typescript.nodes.TypeScriptNode;
import nl.cge.lang.typescript.runtime.Undefined;
import nl.cge.lang.typescript.nodes.exprs.arrays.ArrayIndexReadExprNode;
import nl.cge.lang.typescript.nodes.exprs.functions.FunctionCallExprNode;
import nl.cge.lang.typescript.nodes.exprs.properties.PropertyReadExprNode;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * The abstract common ancestor of all expression Nodes in TypeScript.
 * Identical to the class with the same name from part 16.
 */
@TypeSystemReference(TypeScriptTypeSystem.class)
public abstract class TypeScriptExprNode extends TypeScriptNode {
    public abstract Object executeGeneric(VirtualFrame frame);

    public boolean executeBool(VirtualFrame frame) {
        Object value = this.executeGeneric(frame);
        // 'undefined' is falsy
        if (value == Undefined.INSTANCE) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        // a number is falsy when it's 0
        if (value instanceof Integer) {
            return (Integer) value != 0;
        }
        if (value instanceof Double) {
            return (Double) value != 0.0;
        }
        if (value instanceof TruffleString) {
            return !((TruffleString) value).isEmpty();
        }
        // all other values are truthy
        return true;
    }

    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return TypeScriptTypeSystemGen.expectInteger(this.executeGeneric(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return TypeScriptTypeSystemGen.expectDouble(this.executeGeneric(frame));
    }

    /**
     * A more fine-grained alternative to {@link #executeGeneric}
     * that is used for function and method calls.
     * It returns the value that should be used as the receiver of the given function call
     * (the {@code this} variable).
     * Used in {@link FunctionCallExprNode},
     * alongside {@link #evaluateAsFunction}.
     * The default implementation returns {@link Undefined#INSTANCE},
     * and is overridden only in {@link PropertyReadExprNode}
     * and {@link ArrayIndexReadExprNode}.
     */
    public Object evaluateAsReceiver(VirtualFrame frame) {
        // by default, almost no expressions have a method receiver -
        // the exception is property access
        return Undefined.INSTANCE;
    }

    /**
     * A more fine-grained alternative to {@link #executeGeneric}
     * that is used for function and method calls.
     * Used in {@link FunctionCallExprNode},
     * alongside {@link #evaluateAsReceiver}.
     * The default implementation is to delegate to {@link #executeGeneric},
     * and is overridden only in {@link PropertyReadExprNode}
     * and {@link ArrayIndexReadExprNode}.
     */
    public Object evaluateAsFunction(VirtualFrame frame, Object receiver) {
        // by default, the function is simply the result of executing the expression
        return this.executeGeneric(frame);
    }
}

package nl.cge.lang.typescript.nodes.exprs.literals;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

/**
 * The AST node that represents the 'undefined' literal JavaScript expression.
 * Identical to the class with the same name from part 16.
 */
public final class UndefinedLiteralExprNode extends TypeScriptExprNode {
    @Override
    public boolean executeBool(VirtualFrame frame) {
        // undefined is falsy
        return false;
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        throw new UnexpectedResultException(Undefined.INSTANCE);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        throw new UnexpectedResultException(Undefined.INSTANCE);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return Undefined.INSTANCE;
    }
}

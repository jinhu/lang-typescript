package nl.cge.lang.typescript.nodes.exprs.literals;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * The AST node that represents an integer literal expression in TypeScript.
 * Identical to the class with the same name from part 16.
 */
public final class IntLiteralExprNode extends TypeScriptExprNode {
    private final int value;

    public IntLiteralExprNode(int value) {
        this.value = value;
    }

    @Override
    public boolean executeBool(VirtualFrame frame) {
        return this.value != 0;
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        return this.value;
    }

    @Override
    public double executeDouble(VirtualFrame frame) {
        return this.value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return this.value;
    }
}

package nl.cge.lang.typescript.nodes.stmts.controlflow;

import nl.cge.lang.typescript.exceptions.ReturnException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A Node representing the {@code return} statement.
 * Identical to the class with the same name from part 16.
 */
public final class ReturnStmtNode extends TypeScriptStmtNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptExprNode returnExpr;

    public ReturnStmtNode(TypeScriptExprNode returnExpr, SourceSection sourceSection) {
        super(sourceSection);
        this.returnExpr = returnExpr;
    }

    @Override
    public Object executeStatement(VirtualFrame frame) {
        Object returnValue = this.returnExpr.executeGeneric(frame);
        throw new ReturnException(returnValue);
    }
}

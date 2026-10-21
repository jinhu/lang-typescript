package nl.cge.lang.typescript.nodes.stmts.controlflow;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A Node that represents an {@code if} statement.
 * Identical to the class with the same name from part 16.
 */
public final class IfStmtNode extends TypeScriptStmtNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptExprNode conditionExpr;

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptStmtNode thenStmt;

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptStmtNode elseStmt;

    private final CountingConditionProfile condition = CountingConditionProfile.create();

    public IfStmtNode(
            TypeScriptExprNode conditionExpr, TypeScriptStmtNode thenStmt,
            TypeScriptStmtNode elseStmt, SourceSection sourceSection) {
        super(sourceSection);
        this.conditionExpr = conditionExpr;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    @Override
    public Object executeStatement(VirtualFrame frame) {
        if (this.condition.profile(this.conditionExpr.executeBool(frame))) {
            return this.thenStmt.executeStatement(frame);
        } else {
            return this.elseStmt == null
                    ? Undefined.INSTANCE
                    : this.elseStmt.executeStatement(frame);
        }
    }
}

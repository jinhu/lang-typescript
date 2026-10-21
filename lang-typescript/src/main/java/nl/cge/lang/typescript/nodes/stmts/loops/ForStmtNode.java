package nl.cge.lang.typescript.nodes.stmts.loops;

import nl.cge.lang.typescript.exceptions.BreakException;
import nl.cge.lang.typescript.exceptions.ContinueException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A Node that represents a {@code for} statement.
 * Identical to the class with the same name from part 16.
 */
public final class ForStmtNode extends TypeScriptStmtNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptStmtNode initStmt;

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private LoopNode loopNode;

    public ForStmtNode(TypeScriptStmtNode initStmt, TypeScriptExprNode conditionExpr,
            TypeScriptExprNode updateExpr, TypeScriptStmtNode bodyStmt,
            SourceSection sourceSection) {
        super(sourceSection);
        this.initStmt = initStmt;
        this.loopNode = Truffle.getRuntime().createLoopNode(
                new ForRepeatingNode(conditionExpr, updateExpr, bodyStmt));
    }

    @Override
    public Object executeStatement(VirtualFrame frame) {
        // first, we execute the init statement, if provided
        if (this.initStmt != null) {
            this.initStmt.executeStatement(frame);
        }
        this.loopNode.execute(frame);
        return Undefined.INSTANCE;
    }

    private static final class ForRepeatingNode extends Node implements RepeatingNode {
        @SuppressWarnings("FieldMayBeFinal")
        @Child
        private TypeScriptExprNode conditionExpr;

        @SuppressWarnings("FieldMayBeFinal")
        @Child
        private TypeScriptExprNode updateExpr;

        @SuppressWarnings("FieldMayBeFinal")
        @Child
        private TypeScriptStmtNode bodyStmt;

        public ForRepeatingNode(TypeScriptExprNode conditionExpr, TypeScriptExprNode updateExpr,
                TypeScriptStmtNode bodyStmt) {
            this.conditionExpr = conditionExpr;
            this.updateExpr = updateExpr;
            this.bodyStmt = bodyStmt;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (this.conditionExpr != null &&
                    !this.conditionExpr.executeBool(frame)) {
                return false;
            }
            try {
                this.bodyStmt.executeStatement(frame);
            } catch (BreakException e) {
                // 'break' means 'stop the loop'
                return false;
            } catch (ContinueException e) {
                // fall-through on 'continue'
            }
            if (this.updateExpr != null) {
                this.updateExpr.executeGeneric(frame);
            }
            return true;
        }
    }
}

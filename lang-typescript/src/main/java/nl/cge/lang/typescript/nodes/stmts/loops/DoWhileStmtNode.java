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
 * A Node that represents a {@code do-while} statement.
 * Identical to the class with the same name from part 16.
 */
public final class DoWhileStmtNode extends TypeScriptStmtNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private LoopNode loopNode;

    public DoWhileStmtNode(
            TypeScriptExprNode conditionExpr, TypeScriptStmtNode bodyStmt, SourceSection sourceSection) {
        super(sourceSection);
        this.loopNode = Truffle.getRuntime().createLoopNode(new DoWhileRepeatingNode(conditionExpr, bodyStmt));
    }

    @Override
    public Object executeStatement(VirtualFrame frame) {
        this.loopNode.execute(frame);
        return Undefined.INSTANCE;
    }

    private static final class DoWhileRepeatingNode extends Node implements RepeatingNode {
        @SuppressWarnings("FieldMayBeFinal")
        @Child
        private TypeScriptExprNode conditionExpr;

        @SuppressWarnings("FieldMayBeFinal")
        @Child
        private TypeScriptStmtNode bodyStmt;

        public DoWhileRepeatingNode(TypeScriptExprNode conditionExpr, TypeScriptStmtNode bodyStmt) {
            this.conditionExpr = conditionExpr;
            this.bodyStmt = bodyStmt;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            // in a do-while, we first execute the body of the loop
            try {
                this.bodyStmt.executeStatement(frame);
            } catch (BreakException e) {
                // 'break' means 'stop the loop'
                return false;
            } catch (ContinueException e) {
                // fall-through on 'continue'
            }
            return this.conditionExpr.executeBool(frame);
        }
    }
}

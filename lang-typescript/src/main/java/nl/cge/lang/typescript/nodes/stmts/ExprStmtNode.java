package nl.cge.lang.typescript.nodes.stmts;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A Node that represents an expression statement.
 * Identical to the class with the same name from part 16.
 */
public final class ExprStmtNode extends TypeScriptStmtNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptExprNode expr;

    /**
     * We now reference this field from {@link nl.cge.lang.typescript.nodes.stmts.blocks.LocalVarNodeVisitor},
     * so it needs to be {@code public}.
     */
    public final boolean discardExpressionValue;

    public ExprStmtNode(TypeScriptExprNode expr) {
        this(expr, null);
    }

    /**
     * Creates a new instance of the expression statement.
     *
     * @param expr the expression node
     */
    public ExprStmtNode(TypeScriptExprNode expr, SourceSection sourceSection) {
        this(expr, sourceSection, false);
    }

    /**
     * Creates a new instance of the expression statement.
     *
     * @param expr the expression node
     * @param discardExpressionValue whether evaluating this statement should discard
     *   the value of the expression it wraps,
     *   and always return {@code Undefined.INSTANCE}
     */
    public ExprStmtNode(TypeScriptExprNode expr, SourceSection sourceSection,
            boolean discardExpressionValue) {
        super(sourceSection);
        this.expr = expr;
        this.discardExpressionValue = discardExpressionValue;
    }

    /** Evaluating the statement returns the result of executing its expression. */
    @Override
    public Object executeStatement(VirtualFrame frame) {
        Object exprResult = this.expr.executeGeneric(frame);
        // if this statement was created because of transforming a local variable declaration into an assignment,
        // return 'undefined', to be consistent with how other declarations work
        return this.discardExpressionValue ? Undefined.INSTANCE : exprResult;
    }
}

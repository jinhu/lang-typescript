package nl.cge.lang.typescript.nodes.root;

import nl.cge.lang.typescript.TypeScriptTruffleLanguage;
import nl.cge.lang.typescript.nodes.exprs.functions.built_in.BuiltInFunctionBodyExprNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * The {@link RootNode} for our built-in functions.
 * Simply wraps the expression Node representing the body of the function.
 * Identical to the class with the same name from part 16.
 */
public final class BuiltInFuncRootNode extends RootNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private BuiltInFunctionBodyExprNode functionBodyExpr;

    public BuiltInFuncRootNode(TypeScriptTruffleLanguage truffleLanguage,
            BuiltInFunctionBodyExprNode functionBodyExpr) {
        super(truffleLanguage);

        this.functionBodyExpr = functionBodyExpr;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return this.functionBodyExpr.executeGeneric(frame);
    }
}

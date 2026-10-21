package nl.cge.lang.typescript.nodes.exprs.objects;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * The Node implementing the 'this' expression.
 * Identical to the class with the same name from part 16.
 */
public final class ThisExprNode extends TypeScriptExprNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // because of how we handle calls in FunctionDispatchNode,
        // the `this` object is always in the first argument
        return frame.getArguments()[0];
    }
}

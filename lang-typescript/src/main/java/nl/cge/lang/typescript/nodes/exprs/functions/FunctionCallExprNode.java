package nl.cge.lang.typescript.nodes.exprs.functions;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import java.util.List;

/**
 * The Node representing the expression of calling a function in TypeScript,
 * for example {@code Math.pow(2, 3)}.
 * Identical to the class with the same name from part 16.
 */
public final class FunctionCallExprNode extends TypeScriptExprNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptExprNode targetFunction;

    @Children
    private final TypeScriptExprNode[] callArguments;

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private FunctionDispatchNode dispatchNode;

    public FunctionCallExprNode(TypeScriptExprNode targetFunction, List<TypeScriptExprNode> callArguments) {
        this.targetFunction = targetFunction;
        this.callArguments = callArguments.toArray(new TypeScriptExprNode[]{});
        this.dispatchNode = FunctionDispatchNodeGen.create();
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object receiver = this.targetFunction.evaluateAsReceiver(frame);
        Object function = this.targetFunction.evaluateAsFunction(frame, receiver);

        Object[] argumentValues = new Object[this.callArguments.length];
        for (int i = 0; i < this.callArguments.length; i++) {
            argumentValues[i] = this.callArguments[i].executeGeneric(frame);
        }

        return this.dispatchNode.executeDispatch(function, argumentValues, receiver);
    }
}

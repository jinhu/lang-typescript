package nl.cge.lang.typescript.nodes.exprs.comparisons;

import nl.cge.lang.typescript.nodes.exprs.BinaryOperationExprNode;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * Node class representing the strict inequality ({@code !==}) operator.
 * Identical to the class with the same name from part 16.
 */
public abstract class InequalityExprNode extends BinaryOperationExprNode {
    @Specialization
    protected boolean intInequality(int leftValue, int rightValue) {
        return leftValue != rightValue;
    }

    @Specialization(replaces = "intInequality")
    protected boolean doubleInequality(double leftValue, double rightValue) {
        return leftValue != rightValue;
    }

    @Specialization
    protected boolean boolInequality(boolean leftValue, boolean rightValue) {
        return leftValue != rightValue;
    }

    @Specialization
    protected boolean stringInequality(TruffleString leftValue, TruffleString rightValue,
            @Cached TruffleString.EqualNode equalNode) {
        return !TypeScriptTruffleStrings.equals(leftValue, rightValue, equalNode);
    }

    @Fallback
    protected boolean objectInequality(Object leftValue, Object rightValue) {
        return leftValue != rightValue;
    }
}

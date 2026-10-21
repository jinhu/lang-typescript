package nl.cge.lang.typescript.nodes.exprs.comparisons;

import nl.cge.lang.typescript.nodes.exprs.BinaryOperationExprNode;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * Node class representing the strict equality ({@code ===}) operator.
 * Identical to the class with the same name from part 16.
 */
public abstract class EqualityExprNode extends BinaryOperationExprNode {
    @Specialization
    protected boolean intEquality(int leftValue, int rightValue) {
        return leftValue == rightValue;
    }

    @Specialization(replaces = "intEquality")
    protected boolean doubleEquality(double leftValue, double rightValue) {
        return leftValue == rightValue;
    }

    @Specialization
    protected boolean boolEquality(boolean leftValue, boolean rightValue) {
        return leftValue == rightValue;
    }

    @Specialization
    protected boolean stringEquality(TruffleString leftValue, TruffleString rightValue,
            @Cached TruffleString.EqualNode equalNode) {
        return TypeScriptTruffleStrings.equals(leftValue, rightValue, equalNode);
    }

    @Fallback
    protected boolean objectEquality(Object leftValue, Object rightValue) {
        return leftValue == rightValue;
    }
}

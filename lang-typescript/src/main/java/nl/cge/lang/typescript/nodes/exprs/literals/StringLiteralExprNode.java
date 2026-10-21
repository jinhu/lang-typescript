package nl.cge.lang.typescript.nodes.exprs.literals;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * The AST node that represents a string literal expression in TypeScript.
 * Identical to the class with the same name from part 16.
 */
public final class StringLiteralExprNode extends TypeScriptExprNode {
    private final TruffleString value;

    public StringLiteralExprNode(String value) {
        this.value = TypeScriptTruffleStrings.fromJavaString(value);
    }

    /** Empty strings are considered "falsy" in JavaScript. */
    @Override
    public boolean executeBool(VirtualFrame frame) {
        return !this.value.isEmpty();
    }

    @Override
    public TruffleString executeGeneric(VirtualFrame frame) {
        return this.value;
    }
}

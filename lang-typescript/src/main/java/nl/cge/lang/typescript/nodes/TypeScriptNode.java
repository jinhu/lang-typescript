package nl.cge.lang.typescript.nodes;

import nl.cge.lang.typescript.TypeScriptLanguageContext;
import nl.cge.lang.typescript.TypeScriptTruffleLanguage;
import com.oracle.truffle.api.nodes.Node;

/**
 * The abstract common ancestor of all TypeScript AST Truffle Nodes.
 * Identical to the class with the same name from part 16.
 */
public abstract class TypeScriptNode extends Node {
    /** Allows retrieving the current Truffle language instance from within a Node. */
    protected final TypeScriptTruffleLanguage currentTruffleLanguage() {
        return TypeScriptTruffleLanguage.get(this);
    }

    /** Allows retrieving the current Truffle language Context from within a Node. */
    protected final TypeScriptLanguageContext currentLanguageContext() {
        return TypeScriptLanguageContext.get(this);
    }
}

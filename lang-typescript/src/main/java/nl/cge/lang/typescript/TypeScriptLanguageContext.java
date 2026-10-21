package nl.cge.lang.typescript;

import nl.cge.lang.typescript.common.ShapesAndPrototypes;
import nl.cge.lang.typescript.runtime.FunctionObject;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

/**
 * The class of the context for the
 * {@link TypeScriptTruffleLanguage TruffleLanguage implementaton in this part of the series}.
 * Identical to the class with the same name from part 16.
 */
public final class TypeScriptLanguageContext {
    private static final TruffleLanguage.ContextReference<TypeScriptLanguageContext> REF =
            TruffleLanguage.ContextReference.create(TypeScriptTruffleLanguage.class);

    /** Retrieve the current language context for the given {@link Node}. */
    public static TypeScriptLanguageContext get(Node node) {
        return REF.get(node);
    }

    public final DynamicObject globalScopeObject;

    /**
     * The object containing the shapes and prototypes,
     * both for user-defined class instances, and for built-in objects.
     */
    public final ShapesAndPrototypes shapesAndPrototypes;

    public final FunctionObject emptyFunction;

    public TypeScriptLanguageContext(DynamicObject globalScopeObject,
            ShapesAndPrototypes shapesAndPrototypes, FunctionObject emptyFunction) {
        this.globalScopeObject = globalScopeObject;
        this.shapesAndPrototypes = shapesAndPrototypes;
        this.emptyFunction = emptyFunction;
    }
}

package nl.cge.lang.typescript.nodes.exprs;

import nl.cge.lang.typescript.nodes.TypeScriptNode;
import nl.cge.lang.typescript.runtime.GlobalScopeObject;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

/**
 * A simple expression class that returns the
 * {@link GlobalScopeObject global scope object}
 * using the inherited {@link #currentLanguageContext()} method from
 * {@link TypeScriptNode}.
 * Used by classes that access the global scope,
 * like global variable declaration or assignment,
 * so that they can have this Node as a child,
 * and then use the {@link com.oracle.truffle.api.library.CachedLibrary}
 * annotation in their {@link Specialization} methods.
 * Identical to the class with the same name from part 16.
 */
@GenerateInline(false)
public abstract class GlobalScopeObjectExprNode extends TypeScriptExprNode {
    @Specialization
    protected DynamicObject returnGlobalScopeObject() {
        return this.currentLanguageContext().globalScopeObject;
    }
}

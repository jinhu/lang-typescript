package nl.cge.lang.typescript.nodes.exprs.variables;

import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.exprs.GlobalScopeObjectExprNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

/**
 * A Node that represents the expression of referencing a global variable in TypeScript.
 * Identical to the class with the same name from part 16.
 */
@NodeChild(value = "globalScopeObjectExpr", type = GlobalScopeObjectExprNode.class)
@NodeField(name = "name", type = String.class)
public abstract class GlobalVarReferenceExprNode extends TypeScriptExprNode {
    protected abstract String getName();

    @Specialization(limit = "2")
    protected Object readVariable(DynamicObject globalScopeObject,
            @CachedLibrary("globalScopeObject") DynamicObjectLibrary objectLibrary) {
        String variableId = this.getName();
        var value = objectLibrary.getOrDefault(globalScopeObject, variableId, null);
        if (value == null) {
            throw new TypeScriptException(this, "'" + variableId + "' is not defined");
        } else {
            return value;
        }
    }
}

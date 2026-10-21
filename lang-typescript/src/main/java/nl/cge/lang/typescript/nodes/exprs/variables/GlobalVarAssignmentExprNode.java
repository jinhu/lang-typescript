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
import com.oracle.truffle.api.object.Property;

/**
 * A Node that represents the expression of assigning a value to a global variable in TypeScript.
 * Identical to the class with the same name from part 16.
 */
@NodeChild(value = "globalScopeObjectExpr", type = GlobalScopeObjectExprNode.class)
@NodeChild(value = "assignmentExpr")
@NodeField(name = "name", type = String.class)
public abstract class GlobalVarAssignmentExprNode extends TypeScriptExprNode {
    protected abstract String getName();

    @Specialization(limit = "2")
    protected Object assignVariable(DynamicObject globalScopeObject, Object value,
            @CachedLibrary("globalScopeObject") DynamicObjectLibrary objectLibrary) {
        String variableId = this.getName();
        Property property = objectLibrary.getProperty(globalScopeObject, variableId);
        if (property == null) {
            throw new TypeScriptException(this, "'" + variableId + "' is not defined");
        }
        if (property.getFlags() == 1) {
            throw new TypeScriptException("Assignment to constant variable '" + variableId + "'");
        }
        objectLibrary.put(globalScopeObject, variableId, value);
        return value;
    }
}

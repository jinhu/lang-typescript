package nl.cge.lang.typescript.nodes.exprs.properties;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;

/**
 * The Node for writing properties of objects.
 * Used in code like {@code t.myProp = 3}.
 * Very similar to {@link PropertyReadExprNode}.
 * Simply delegates to {@link CommonWritePropertyNode}.
 * Identical to the class with the same name from part 16.
 */
@NodeChild("targetExpr")
@NodeField(name = "propertyName", type = String.class)
@NodeChild("rvalueExpr")
public abstract class PropertyWriteExprNode extends TypeScriptExprNode {
    protected abstract String getPropertyName();

    @Specialization
    protected Object writeProperty(Object target, Object rvalue,
            @Cached(inline = true) CommonWritePropertyNode commonWritePropertyNode) {
        return commonWritePropertyNode.executeWriteProperty(this, target, this.getPropertyName(), rvalue);
    }
}

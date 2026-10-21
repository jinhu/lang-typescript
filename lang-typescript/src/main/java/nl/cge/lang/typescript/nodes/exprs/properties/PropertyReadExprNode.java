package nl.cge.lang.typescript.nodes.exprs.properties;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.exprs.objects.SuperExprNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * The Node for reading properties of objects.
 * Used in code like {@code t.myProp}.
 * Identical to the class with the same name from part 16.
 */
@NodeChild("targetExpr")
@NodeField(name = "propertyName", type = String.class)
public abstract class PropertyReadExprNode extends TypeScriptExprNode {
    protected abstract TypeScriptExprNode getTargetExpr();
    protected abstract String getPropertyName();

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private CommonReadPropertyNode commonReadPropertyNode = CommonReadPropertyNodeGen.create();

    @Specialization
    protected Object readProperty(Object target) {
        return this.commonReadPropertyNode.executeReadProperty(
                this, target, this.getPropertyName());
    }

    @Override
    public Object evaluateAsReceiver(VirtualFrame frame) {
        return this.getTargetExpr().executeGeneric(frame);
    }

    @Override
    public Object evaluateAsFunction(VirtualFrame frame, Object receiver) {
        TypeScriptExprNode targetExpr = this.getTargetExpr();
        // if we're reading a property of 'super',
        // we know we need to look in its parent prototype,
        // and not in 'this' (which will be used as the method receiver)
        Object propertyTarget = targetExpr instanceof SuperExprNode
                ? ((SuperExprNode) targetExpr).readParentPrototype()
                : receiver;
        return this.readProperty(propertyTarget);
    }
}

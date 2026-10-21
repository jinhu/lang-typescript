package nl.cge.lang.typescript.nodes.exprs.arrays;

import nl.cge.lang.typescript.common.ShapesAndPrototypes;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.runtime.ArrayObject;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import java.util.List;

/**
 * The Node representing array literal expressions
 * (like {@code [1, 2, 3]}).
 * Identical to the class with the same name from part 16.
 */
public final class ArrayLiteralExprNode extends TypeScriptExprNode {
    @Children
    private final TypeScriptExprNode[] arrayElementExprs;

    public ArrayLiteralExprNode(List<TypeScriptExprNode> arrayElementExprs) {
        this.arrayElementExprs = arrayElementExprs.toArray(new TypeScriptExprNode[]{});
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] arrayElements = new Object[this.arrayElementExprs.length];
        for (var i = 0; i < this.arrayElementExprs.length; i++) {
            arrayElements[i] = this.arrayElementExprs[i].executeGeneric(frame);
        }
        ShapesAndPrototypes shapesAndPrototypes = this.currentLanguageContext().shapesAndPrototypes;
        return new ArrayObject(shapesAndPrototypes.arrayShape,
                shapesAndPrototypes.arrayPrototype, arrayElements);
    }
}

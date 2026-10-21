package nl.cge.lang.typescript.nodes.stmts.variables;

import nl.cge.lang.typescript.nodes.exprs.functions.FuncDefExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;

/**
 * A statement that represents a function declaration nested inside another function
 * (or method).
 */
@NodeChild(value = "funcDefExpr", type = FuncDefExprNode.class)
@NodeField(name = "nestedFuncFrameSlot", type = int.class)
public abstract class NestedFuncDeclStmtNode extends TypeScriptStmtNode {
    protected abstract int getNestedFuncFrameSlot();

    protected NestedFuncDeclStmtNode() {
        super(null);
    }

    @Specialization
    protected Object declareNestedFunction(VirtualFrame frame, Object func) {
        // we need to save the function object resulting from executing the child
        // FuncDefExprNode as a local variable of the parent function
        frame.setObject(this.getNestedFuncFrameSlot(), func);

        return Undefined.INSTANCE;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return false;
    }
}

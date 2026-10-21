package nl.cge.lang.typescript.nodes.stmts.blocks;

import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.Undefined;
import nl.cge.lang.typescript.runtime.debugger.RefObject;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import java.util.List;

/**
 * A Node for representing the TypeScript program itself,
 * and any statement blocks,
 * for example those used as the branch of an {@code if} statement
 * (with the exception of the block for a user-defined function's body,
 * which is represented by {@link UserFuncBodyStmtNode}).
 * Identical to the class with the same name from part 16.
 */
public final class BlockStmtNode extends TypeScriptStmtNode {
    @Children
    private final TypeScriptStmtNode[] stmts;

    private final boolean programBlock;

    @CompilationFinal(dimensions = 1)
    private RefObject[] findLocalVarRefsCache;

    public BlockStmtNode(List<TypeScriptStmtNode> stmts) {
        this(stmts, null);
    }

    public BlockStmtNode(List<TypeScriptStmtNode> stmts, SourceSection sourceSection) {
        this(stmts, false, sourceSection);
    }

    public BlockStmtNode(List<TypeScriptStmtNode> stmts, boolean programBlock,
            SourceSection sourceSection) {
        super(sourceSection);
        this.stmts = stmts.toArray(new TypeScriptStmtNode[]{});
        this.programBlock = programBlock;
    }

    /**
     * Evaluating the block statement evaluates all statements inside it,
     * and returns the result of executing the last statement.
     */
    @Override
    @ExplodeLoop
    public Object executeStatement(VirtualFrame frame) {
        int stmtsMinusOne = this.stmts.length - 1;
        for (int i = 0; i < stmtsMinusOne; i++) {
            this.stmts[i].executeStatement(frame);
        }
        return stmtsMinusOne < 0 ? Undefined.INSTANCE : this.stmts[stmtsMinusOne].executeStatement(frame);
    }

    /**
     * Block statements for the top-level program need to be marked with the Root tag.
     */
    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        // this makes the (real) debugger stop at the beginning of the program,
        // instead on the first non-function declaration statement
//        if (this.programBlock && tag == StandardTags.RootTag.class) {
//            return true;
//        }
//        return super.hasTag(tag);
        return this.programBlock && tag == StandardTags.RootTag.class;
    }

    public RefObject[] getLocalVarRefs() {
        if (this.findLocalVarRefsCache == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.findLocalVarRefsCache = this.findLocalVarRefs();
        }
        return this.findLocalVarRefsCache;
    }

    private RefObject[] findLocalVarRefs() {
        var localVarNodeVisitor = new LocalVarNodeVisitor();
        NodeUtil.forEachChild(this, localVarNodeVisitor);
        RefObject[] variables = localVarNodeVisitor.localVarRefs.toArray(new RefObject[0]);

        Node parentBlock = this.findParentBlock();
        RefObject[] parentVariables = parentBlock instanceof BlockStmtNode
                ? ((BlockStmtNode) parentBlock).getLocalVarRefs()
                : (parentBlock instanceof UserFuncBodyStmtNode
                    ? ((UserFuncBodyStmtNode) parentBlock).getFuncArgAndLocalVarRefs()
                    : null);
        if (parentVariables == null || parentVariables.length == 0) {
            return variables;
        }

        RefObject[] allVariables = new RefObject[variables.length + parentVariables.length];
        System.arraycopy(variables, 0, allVariables, 0, variables.length);
        System.arraycopy(parentVariables, 0, allVariables, variables.length, parentVariables.length);
        return allVariables;
    }
}

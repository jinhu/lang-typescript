package nl.cge.lang.typescript.runtime.debugger;

import nl.cge.lang.typescript.nodes.root.StmtBlockRootNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.BlockStmtNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.UserFuncBodyStmtNode;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * A debugger scope for a block statement.
 * This can be either any block except the top-level block of a user-defined function
 * (which is handled by {@link FuncDebuggerScopeObject}),
 * or any block (including the top-level one) in the main program.
 * Identical to the class with the same name from part 16.
 */
@ExportLibrary(InteropLibrary.class)
public final class BlockDebuggerScopeObject extends AbstractDebuggerScopeObject {
    // needs to be package-private,
    // as it's used in @Cached expressions
    final BlockStmtNode blockStmtNode;

    public BlockDebuggerScopeObject(BlockStmtNode blockStmtNode, Frame frame) {
        super(frame);
        this.blockStmtNode = blockStmtNode;
    }

    @Override
    protected RefObject[] getReferences() {
        return this.blockStmtNode.getLocalVarRefs();
    }

    @ExportMessage
    Object toDisplayString(
            @SuppressWarnings("unused") boolean allowSideEffects,
            @Cached(value = "this.blockStmtNode.findParentBlock()", adopt = false, allowUncached = true, neverDefault = true) @Shared("nodeGrandParentBlock") Node nodeGrandParentBlock
    ) {
        return nodeGrandParentBlock instanceof RootNode
                ? ((RootNode) nodeGrandParentBlock).getName()
                : "block";
    }

    @ExportMessage
    boolean hasScopeParent(
            @Cached(value = "this.blockStmtNode.findParentBlock()", adopt = false, allowUncached = true, neverDefault = true) @Shared("nodeGrandParentBlock") Node nodeGrandParentBlock) {
        return !(nodeGrandParentBlock instanceof StmtBlockRootNode);
    }

    @ExportMessage
    Object getScopeParent(
            @Cached(value = "this.blockStmtNode.findParentBlock()", adopt = false, allowUncached = true, neverDefault = true) @Shared("nodeGrandParentBlock") Node nodeGrandParentBlock)
            throws UnsupportedMessageException {
        if (nodeGrandParentBlock instanceof BlockStmtNode) {
            return new BlockDebuggerScopeObject((BlockStmtNode) nodeGrandParentBlock, this.frame);
        } else if (nodeGrandParentBlock instanceof UserFuncBodyStmtNode) {
            return new FuncDebuggerScopeObject((UserFuncBodyStmtNode) nodeGrandParentBlock, this.frame);
        } else {
            throw UnsupportedMessageException.create();
        }
    }
}

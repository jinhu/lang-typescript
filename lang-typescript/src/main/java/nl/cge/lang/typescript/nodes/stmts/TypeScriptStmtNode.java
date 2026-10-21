package nl.cge.lang.typescript.nodes.stmts;

import nl.cge.lang.typescript.nodes.TypeScriptNode;
import nl.cge.lang.typescript.nodes.root.StmtBlockRootNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.BlockStmtNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.UserFuncBodyStmtNode;
import nl.cge.lang.typescript.runtime.debugger.BlockDebuggerScopeObject;
import nl.cge.lang.typescript.runtime.debugger.FuncDebuggerScopeObject;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

/**
 * The abstract common ancestor of all AST Nodes that represent statements in TypeScript,
 * like declaring a variable or constant.
 * Identical to the class with the same name from part 16.
 */
@GenerateWrapper
@ExportLibrary(value = NodeLibrary.class)
public abstract class TypeScriptStmtNode extends TypeScriptNode implements InstrumentableNode {
    private final SourceSection sourceSection;

    protected TypeScriptStmtNode(SourceSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    /**
     * Evaluates this statement, and returns the result of executing it.
     */
    public abstract Object executeStatement(VirtualFrame frame);

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new TypeScriptStmtNodeWrapper(this.sourceSection, this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    @Override
    public SourceSection getSourceSection() {
        return this.sourceSection;
    }

    /**
     * Find block of this node. Traverse the parent chain and find the first {@link BlockStmtNode}.
     * If none is found, {@link RootNode} is returned.
     *
     * @return the block node, always non-null. Either {@link BlockStmtNode},
     * {@link UserFuncBodyStmtNode}, or {@link StmtBlockRootNode}
     */
    public final Node findParentBlock() {
        Node parent = this.getParent();
        while (parent != null) {
            if (parent instanceof BlockStmtNode || parent instanceof UserFuncBodyStmtNode) {
                break;
            }
            Node grandParent = parent.getParent();
            if (grandParent == null) {
                // we know that parent is a RootNode here
                // (specifically, a StmtBlockRootNode)
                break;
            }
            parent = grandParent;
        }
        return parent;
    }

    @ExportMessage
    boolean hasScope(
            @SuppressWarnings("unused") Frame frame,
            @Cached(value = "this.findParentBlock()", adopt = false, allowUncached = true, neverDefault = true) @Shared("thisParentBlock") Node thisParentBlock) {
        return !(thisParentBlock instanceof StmtBlockRootNode);
    }

    /**
     * Create a debugger scope for the current statement.
     * If the statement is placed on the first level of a user-defined function,
     * we return an instance of {@link FuncDebuggerScopeObject} -
     * for statements on the second or lower level of a user-defined function,
     * or for statements inside the main program block, we return an instance of {@link BlockDebuggerScopeObject}.
     */
    @ExportMessage
    Object getScope(
            Frame frame,
            @SuppressWarnings("unused") boolean nodeEnter,
            @Cached(value = "this.findParentBlock()", adopt = false, allowUncached = true, neverDefault = true) @Shared("thisParentBlock") Node thisParentBlock) {
        return thisParentBlock instanceof BlockStmtNode
                ? new BlockDebuggerScopeObject((BlockStmtNode) thisParentBlock, frame)
                : new FuncDebuggerScopeObject((UserFuncBodyStmtNode) thisParentBlock, frame);
    }
}

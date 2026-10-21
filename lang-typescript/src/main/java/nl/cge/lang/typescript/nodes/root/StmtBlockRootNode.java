package nl.cge.lang.typescript.nodes.root;

import nl.cge.lang.typescript.TypeScriptTruffleLanguage;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.BlockStmtNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.UserFuncBodyStmtNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A {@link RootNode} that represents the execution of a block of statements.
 * Used as the {@link RootNode} for the entire TypeScript program,
 * and also for user-defined functions.
 * Identical to the class with the same name from part 16.
 */
public final class StmtBlockRootNode extends RootNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private TypeScriptStmtNode blockStmt;

    private final String name;
    private final SourceSection sourceSection;

    public StmtBlockRootNode(TypeScriptTruffleLanguage truffleLanguage,
            FrameDescriptor frameDescriptor, BlockStmtNode blockStmt) {
        this(truffleLanguage, frameDescriptor, blockStmt, null, null);
    }

    public StmtBlockRootNode(TypeScriptTruffleLanguage truffleLanguage,
            FrameDescriptor frameDescriptor, BlockStmtNode blockStmt,
            String name, SourceSection sourceSection) {
        this(truffleLanguage, frameDescriptor, (TypeScriptStmtNode) blockStmt, name, sourceSection);
    }

    public StmtBlockRootNode(TypeScriptTruffleLanguage truffleLanguage,
            FrameDescriptor frameDescriptor, UserFuncBodyStmtNode blockStmt,
            String name) {
        this(truffleLanguage, frameDescriptor, blockStmt, name, blockStmt.getSourceSection());
    }

    private StmtBlockRootNode(TypeScriptTruffleLanguage truffleLanguage,
            FrameDescriptor frameDescriptor, TypeScriptStmtNode blockStmt,
            String name, SourceSection sourceSection) {
        super(truffleLanguage, frameDescriptor);

        this.blockStmt = blockStmt;
        this.name = name;
        this.sourceSection = sourceSection;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return this.blockStmt.executeStatement(frame);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public SourceSection getSourceSection() {
        return this.sourceSection;
    }
}

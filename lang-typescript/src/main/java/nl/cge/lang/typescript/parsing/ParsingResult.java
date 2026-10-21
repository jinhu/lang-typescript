package nl.cge.lang.typescript.parsing;

import nl.cge.lang.typescript.nodes.stmts.blocks.BlockStmtNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;

/**
 * The class used as the result of parsing in
 * {@link TypeScriptTruffleParser#parse}.
 * Identical to the class with the same name from part 16.
 */
public final class ParsingResult {
    /** The Node representing the list of statements that the TypeScript program consists of. */
    public final BlockStmtNode programStmtBlock;

    /**
     * The {@link FrameDescriptor} used for the Truffle program itself
     * (which can contain local variables in this part).
     */
    public final FrameDescriptor topLevelFrameDescriptor;

    public final SourceSection programSourceSection;

    public ParsingResult(BlockStmtNode programStmtBlock, FrameDescriptor topLevelFrameDescriptor,
            SourceSection programSourceSection) {
        this.programStmtBlock = programStmtBlock;
        this.topLevelFrameDescriptor = topLevelFrameDescriptor;
        this.programSourceSection = programSourceSection;
    }
}

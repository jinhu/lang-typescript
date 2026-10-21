package nl.cge.lang.typescript.nodes.stmts.controlflow;

import nl.cge.lang.typescript.exceptions.BreakException;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A Node representing the {@code break} statement.
 * Identical to the class with the same name from part 16.
 */
public final class BreakStmtNode extends TypeScriptStmtNode {
    public BreakStmtNode(SourceSection sourceSection) {
        super(sourceSection);
    }

    @Override
    public Object executeStatement(VirtualFrame frame) {
        throw new BreakException();
    }
}

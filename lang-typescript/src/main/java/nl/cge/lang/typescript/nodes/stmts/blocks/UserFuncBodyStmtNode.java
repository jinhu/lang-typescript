package nl.cge.lang.typescript.nodes.stmts.blocks;

import nl.cge.lang.typescript.exceptions.ReturnException;
import nl.cge.lang.typescript.nodes.exprs.functions.ReadFunctionArgExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.Undefined;
import nl.cge.lang.typescript.runtime.debugger.FuncArgRefObject;
import nl.cge.lang.typescript.runtime.debugger.RefObject;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.source.SourceSection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Node for representing the statement blocks of a user-defined function in TypeScript.
 * Returns its value by catching {@link ReturnException}.
 * Identical to the class with the same name from part 16.
 */
public final class UserFuncBodyStmtNode extends TypeScriptStmtNode {
    @Children
    private final TypeScriptStmtNode[] stmts;

    @CompilationFinal(dimensions = 1)
    private RefObject[] findFuncArgRefsCache;

    public UserFuncBodyStmtNode(
            List<TypeScriptStmtNode> stmts, SourceSection sourceSection) {
        super(sourceSection);
        this.stmts = stmts.toArray(new TypeScriptStmtNode[]{});
    }

    /**
     * Evaluating the block statement evaluates all statements inside it,
     * and returns whatever a 'return' statement inside it returns.
     */
    @Override
    @ExplodeLoop
    public Object executeStatement(VirtualFrame frame) {
        for (TypeScriptStmtNode stmt : this.stmts) {
            try {
                stmt.executeStatement(frame);
            } catch (ReturnException e) {
                return e.returnValue;
            }
        }
        // if there was no return statement,
        // then we return 'undefined'
        return Undefined.INSTANCE;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.RootTag.class;
    }

    public RefObject[] getFuncArgAndLocalVarRefs() {
        if (this.findFuncArgRefsCache == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.findFuncArgRefsCache = this.findFuncArgAndLocalVarRefs();
        }
        return this.findFuncArgRefsCache;
    }

    private RefObject[] findFuncArgAndLocalVarRefs() {
        Set<FuncArgRefObject> funcArgs = new HashSet<>();
        // The first argument is always special - it represents 'this'.
        // We'll never encounter 'this' below, because we check for ReadFunctionArgExprNode,
        // while 'this' has its own Node (ThisExprNode)
        funcArgs.add(new FuncArgRefObject("this", null, 0));
        NodeUtil.forEachChild(this, new NodeVisitor() {
            @Override
            public boolean visit(Node visitedNode) {
                if (visitedNode instanceof ReadFunctionArgExprNode) {
                    var readFunctionArgExprNode = (ReadFunctionArgExprNode) visitedNode;
                    funcArgs.add(new FuncArgRefObject(
                            readFunctionArgExprNode.argName,
                            readFunctionArgExprNode.getSourceSection(),
                            readFunctionArgExprNode.index));
                    return true;
                }
                return NodeUtil.forEachChild(visitedNode, this);
            }
        });

        var localVarNodeVisitor = new LocalVarNodeVisitor();
        NodeUtil.forEachChild(this, localVarNodeVisitor);

        var allReferences = new RefObject[funcArgs.size() + localVarNodeVisitor.localVarRefs.size()];
        var i = 0;
        for (var funcArg : funcArgs) {
            allReferences[i++] = funcArg;
        }
        for (var localVar : localVarNodeVisitor.localVarRefs) {
            allReferences[i++] = localVar;
        }
        return allReferences;
    }
}

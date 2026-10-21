package nl.cge.lang.typescript.nodes.exprs.functions;

import nl.cge.lang.typescript.common.ShapesAndPrototypes;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.root.StmtBlockRootNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.UserFuncBodyStmtNode;
import nl.cge.lang.typescript.runtime.FunctionObject;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * An expression that represents the definition of a function.
 * Used by {@link nl.cge.lang.typescript.nodes.stmts.variables.FuncDeclStmtNode}
 * and {@link nl.cge.lang.typescript.nodes.stmts.variables.NestedFuncDeclStmtNode}.
 */
public final class FuncDefExprNode extends TypeScriptExprNode {
    private final FrameDescriptor frameDescriptor;
    private final UserFuncBodyStmtNode funcBody;
    private final String funcName;
    private final int argumentCount;
    private final boolean isClosure;

    @CompilationFinal
    private CallTarget cachedCallTarget;

    @CompilationFinal
    private FunctionObject cachedFunction;

    public FuncDefExprNode(FrameDescriptor frameDescriptor, UserFuncBodyStmtNode funcBody,
                String funcName, int argumentCount, boolean isClosure) {
        this.frameDescriptor = frameDescriptor;
        this.funcBody = funcBody;
        this.funcName = funcName;
        this.argumentCount = argumentCount;
        this.isClosure = isClosure;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (this.cachedCallTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            var truffleLanguage = this.currentTruffleLanguage();
            var funcRootNode = new StmtBlockRootNode(truffleLanguage,
                    this.frameDescriptor, this.funcBody, this.funcName);
            this.cachedCallTarget = funcRootNode.getCallTarget();

            if (!this.isClosure) {
                ShapesAndPrototypes shapesAndPrototypes = this.currentLanguageContext().shapesAndPrototypes;
                this.cachedFunction = new FunctionObject(shapesAndPrototypes.rootShape,
                        shapesAndPrototypes.functionPrototype,
                        this.cachedCallTarget, this.argumentCount);
            }
        }

        if (!this.isClosure) {
            return this.cachedFunction;
        } else {
            MaterializedFrame materializedFrame = frame.materialize();
            ShapesAndPrototypes shapesAndPrototypes = this.currentLanguageContext().shapesAndPrototypes;
            return new FunctionObject(shapesAndPrototypes.rootShape, shapesAndPrototypes.functionPrototype,
                    this.cachedCallTarget, this.argumentCount, materializedFrame);
        }
    }
}

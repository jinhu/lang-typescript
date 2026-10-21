package nl.cge.lang.typescript.nodes.exprs.objects;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import java.util.List;

/**
 * The Node for handling a class declaration.
 * It simply handles methods inside the class by delegating to
 * {@link nl.cge.lang.typescript.nodes.stmts.variables.FuncDeclStmtNode}.
 * Identical to the class with the same name from part 16.
 */
public final class ClassDeclExprNode extends TypeScriptExprNode {
    @Children
    private final TypeScriptStmtNode[] classMethodDecls;

    private final ClassPrototypeObject classPrototypeObject;

    public ClassDeclExprNode(List<TypeScriptStmtNode> classMethodDecls,
            ClassPrototypeObject classPrototypeObject) {
        this.classMethodDecls = classMethodDecls.toArray(TypeScriptStmtNode[]::new);
        this.classPrototypeObject = classPrototypeObject;
    }

    @Override
    @ExplodeLoop
    public ClassPrototypeObject executeGeneric(VirtualFrame frame) {
        for (var classMethodDecl : this.classMethodDecls) {
            classMethodDecl.executeStatement(frame);
        }

        return this.classPrototypeObject;
    }
}

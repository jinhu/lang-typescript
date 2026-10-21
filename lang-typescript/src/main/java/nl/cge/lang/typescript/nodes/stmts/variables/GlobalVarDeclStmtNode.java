package nl.cge.lang.typescript.nodes.stmts.variables;

import nl.cge.lang.typescript.common.DeclarationKind;
import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.exprs.GlobalScopeObjectExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A Node that represents the declaration of a global
 * (as opposed to local to a function) variable or constant in TypeScript.
 * Identical to the class with the same name from part 16.
 */
@NodeChild(value = "globalScopeObjectExpr", type = GlobalScopeObjectExprNode.class)
@NodeChild(value = "initializerExpr", type = TypeScriptExprNode.class)
@NodeField(name = "name", type = String.class)
@NodeField(name = "declarationKind", type = DeclarationKind.class)
public abstract class GlobalVarDeclStmtNode extends TypeScriptStmtNode {
    protected abstract String getName();
    protected abstract DeclarationKind getDeclarationKind();

    @CompilationFinal
    private boolean checkVariableExists = true;

    protected GlobalVarDeclStmtNode(SourceSection sourceSection) {
        super(sourceSection);
    }

    @Specialization(limit = "2")
    protected Object createVariable(DynamicObject globalScopeObject, Object value,
            @CachedLibrary("globalScopeObject") DynamicObjectLibrary objectLibrary) {
        var variableId = this.getName();

        if (this.checkVariableExists) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.checkVariableExists = false;

            if (objectLibrary.containsKey(globalScopeObject, variableId)) {
                throw new TypeScriptException(this, "Identifier '" + variableId + "' has already been declared");
            }
        }

        int flags = this.getDeclarationKind() == DeclarationKind.CONST ? 1 : 0;
        objectLibrary.putWithFlags(globalScopeObject, variableId, value, flags);

        // we return 'undefined' for statements that declare variables
        return Undefined.INSTANCE;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        // Global variables representing class declarations don't provide a SourceSection,
        // since we don't want the debugger to stop on them.
        // For that reason, make sure to return the standard Statement tag only if we have a SourceSection
        return this.getSourceSection() != null && super.hasTag(tag);
    }
}

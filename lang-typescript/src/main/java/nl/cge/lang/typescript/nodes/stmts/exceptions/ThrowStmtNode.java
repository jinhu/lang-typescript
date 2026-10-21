package nl.cge.lang.typescript.nodes.stmts.exceptions;

import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.stmts.TypeScriptStmtNode;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import nl.cge.lang.typescript.runtime.JavaScriptObject;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;

import java.util.List;

/**
 * This Node represents the implementation of the {@code throw} statement.
 * Identical to the class with the same name from part 16.
 */
public abstract class ThrowStmtNode extends TypeScriptStmtNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child
    @Executed
    protected TypeScriptExprNode exceptionExpr;

    protected ThrowStmtNode(TypeScriptExprNode exceptionExpr, SourceSection sourceSection) {
        super(sourceSection);
        this.exceptionExpr = exceptionExpr;
    }

    @Specialization(limit = "2")
    protected Object throwJavaScriptObject(
            JavaScriptObject value,
            @CachedLibrary("value") DynamicObjectLibrary nameObjectLibrary,
            @CachedLibrary("value") DynamicObjectLibrary messageObjectLibrary,
            @CachedLibrary("value") DynamicObjectLibrary stackObjectLibrary) {
        Object name = nameObjectLibrary.getOrDefault(value, "name", null);
        Object message = messageObjectLibrary.getOrDefault(value, "message", null);
        var TypeScriptException = new TypeScriptException(name, message, value, this);
        stackObjectLibrary.put(value, "stack", this.formStackTrace(name, message, TypeScriptException));
        throw TypeScriptException;
    }

    @Specialization
    protected Object throwNonJavaScriptObject(Object value) {
        throw new TypeScriptException(value, this);
    }

    @TruffleBoundary
    private TruffleString formStackTrace(Object name, Object message, TypeScriptException TypeScriptException) {
        TruffleStringBuilder sb = TypeScriptTruffleStrings.builder();
        sb.appendJavaStringUTF16Uncached(String.valueOf(name));
        if (message != Undefined.INSTANCE) {
            sb.appendJavaStringUTF16Uncached(": ");
            sb.appendJavaStringUTF16Uncached(String.valueOf(message));
        }
        List<TruffleStackTraceElement> truffleStackTraceEls = TruffleStackTrace.getStackTrace(TypeScriptException);
        for (TruffleStackTraceElement truffleStackTracEl : truffleStackTraceEls) {
            sb.appendJavaStringUTF16Uncached("\n\tat ");

            Node location = truffleStackTracEl.getLocation();
            RootNode rootNode = location.getRootNode();
            String funcName = rootNode.getName();
            // we want to ignore the top-level program RootNode name in this stack trace
            boolean isFunc = !":program".equals(funcName);
            if (isFunc) {
                sb.appendJavaStringUTF16Uncached(funcName);
                sb.appendJavaStringUTF16Uncached(" (");
            }

            SourceSection sourceSection = location.getEncapsulatingSourceSection();
            sb.appendJavaStringUTF16Uncached(sourceSection.getSource().getName());
            sb.appendJavaStringUTF16Uncached(":");
            sb.appendJavaStringUTF16Uncached(String.valueOf(sourceSection.getStartLine()));
            sb.appendJavaStringUTF16Uncached(":");
            sb.appendJavaStringUTF16Uncached(String.valueOf(sourceSection.getStartColumn()));

            if (isFunc) {
                sb.appendJavaStringUTF16Uncached(")");
            }
        }
        return sb.toStringUncached();
    }
}

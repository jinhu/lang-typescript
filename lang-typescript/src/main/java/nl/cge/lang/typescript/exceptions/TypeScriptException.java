package nl.cge.lang.typescript.exceptions;

import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import nl.cge.lang.typescript.runtime.ErrorJavaScriptObject;
import nl.cge.lang.typescript.runtime.JavaScriptObject;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;

/**
 * The exception that's thrown from the TypeScript implementation if any semantic errors are found.
 * Identical to the class with the same name from part 16.
 */
public final class TypeScriptException extends AbstractTruffleException {
    public final Object value;

    /**
     * This constructor is used only for indicating built-in errors,
     * like extending a class that doesn't exist.
     */
    public TypeScriptException(String message) {
        this(null, message);
    }

    /**
     * This constructor is used only for indicating built-in errors,
     * like assigning to a global variable that doesn't exist.
     */
    public TypeScriptException(Node location, String message) {
        super(message, location);

        this.value = null;
    }

    /**
     * The constructor used in the {@code throw} statement,
     * for the specialization that handles raising an object.
     *
     * @see nl.cge.lang.typescript.nodes.stmts.exceptions.ThrowStmtNode
     */
    public TypeScriptException(Object name, Object message, JavaScriptObject javaScriptObject, Node node) {
        super(TypeScriptTruffleStrings.toString(name) + ": " + TypeScriptTruffleStrings.toString(message), node);

        this.value = javaScriptObject;
    }

    /**
     * The constructor used in the {@code throw} statement,
     * for the specialization that handles raising anything that is not an object.
     *
     * @see nl.cge.lang.typescript.nodes.stmts.exceptions.ThrowStmtNode
     */
    public TypeScriptException(Object value, Node node) {
        super(TypeScriptTruffleStrings.toString(value), node);

        this.value = value;
    }

    /**
     * The constructor used for built-in errors,
     * like referencing a property of {@code undefined}.
     */
    public TypeScriptException(ErrorJavaScriptObject errorJavaScriptObject, Node node) {
        super(errorJavaScriptObject.name + ": " + errorJavaScriptObject.message, node);

        this.value = errorJavaScriptObject;
    }
}

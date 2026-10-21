package nl.cge.lang.typescript.nodes.exprs.strings;

import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.TypeScriptNode;
import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * An AST node that represents reading properties of strings.
 * Identical to the class with the same name from part 16.
 */
@GenerateInline(true)
public abstract class ReadTruffleStringPropertyNode extends TypeScriptNode {
    public static final String LENGTH_PROP = "length";

    /** The abstract {@code execute*()} method for this node. */
    public abstract Object executeReadTruffleStringProperty(Node node, TruffleString truffleString, Object property);

    /**
     * The specialization used when accessing an integer index of a string,
     * in code like {@code "abc"[1]}.
     */
    @Specialization
    protected Object readStringIndex(
            TruffleString truffleString,
            int index,
            @Cached(inline = false) @Shared TruffleString.CodePointLengthNode lengthNode,
            @Cached(inline = false) TruffleString.SubstringNode substringNode) {
        return index < 0 || index >= TypeScriptTruffleStrings.length(truffleString, lengthNode)
                ? Undefined.INSTANCE
                : TypeScriptTruffleStrings.substring(truffleString, index, 1, substringNode);
    }

    /**
     * The specialization used when accessing the {@code length}
     * property of a string, in code like {@code "abc".length}
     * or {@code "abc"['length']}.
     */
    @Specialization(guards = "LENGTH_PROP.equals(propertyName)")
    protected int readLengthProperty(
            TruffleString truffleString,
            @SuppressWarnings("unused") String propertyName,
            @Cached(inline = false) @Shared TruffleString.CodePointLengthNode lengthNode) {
        return TypeScriptTruffleStrings.length(truffleString, lengthNode);
    }

    @Fallback
    protected Object readNonLengthProperty(
            @SuppressWarnings("unused") TruffleString truffleString,
            Object property,
            @Cached(value = "currentLanguageContext().shapesAndPrototypes.stringPrototype", neverDefault = true) ClassPrototypeObject stringPrototype,
            @CachedLibrary("stringPrototype") InteropLibrary interopLibrary) {
        try {
            return interopLibrary.readMember(stringPrototype,
                    TypeScriptTruffleStrings.toStringOfMaybeString(property));
        } catch (UnknownIdentifierException e) {
            return Undefined.INSTANCE;
        } catch (UnsupportedMessageException e) {
            throw new TypeScriptException(this, e.getMessage());
        }
    }
}

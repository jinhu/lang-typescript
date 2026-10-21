package nl.cge.lang.typescript.nodes.exprs.properties;

import nl.cge.lang.typescript.common.ShapesAndPrototypes;
import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.TypeScriptNode;
import nl.cge.lang.typescript.nodes.exprs.arrays.ArrayIndexReadExprNode;
import nl.cge.lang.typescript.nodes.exprs.strings.ReadTruffleStringPropertyNode;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import nl.cge.lang.typescript.runtime.ErrorJavaScriptObject;
import nl.cge.lang.typescript.runtime.ObjectPrototype;
import nl.cge.lang.typescript.runtime.Undefined;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * A Node for reading a property of a JavaScript object.
 * Used by {@link PropertyReadExprNode} and {@link ArrayIndexReadExprNode}.
 * Identical to the class with the same name from part 16.
 */
@GenerateInline(true)
public abstract class CommonReadPropertyNode extends TypeScriptNode {
    public abstract Object executeReadProperty(Node node, Object target, Object property);

    /**
     * The specialization for reading a property of a {@link TruffleString}.
     * Simply delegates to {@link ReadTruffleStringPropertyNode}.
     */
    @Specialization
    protected static Object readPropertyOfString(Node node, TruffleString target, Object property,
            @Cached ReadTruffleStringPropertyNode readStringPropertyNode) {
        return readStringPropertyNode.executeReadTruffleStringProperty(
                node, target, property);
    }

    @Specialization(guards = "interopLibrary.hasMembers(target)", limit = "2")
    protected static Object readProperty(Node node, Object target, String propertyName,
            @CachedLibrary("target") InteropLibrary interopLibrary) {
        try {
            return interopLibrary.readMember(target, propertyName);
        } catch (UnknownIdentifierException e) {
            return Undefined.INSTANCE;
        } catch (UnsupportedMessageException e) {
            throw new TypeScriptException(node, e.getMessage());
        }
    }

    /**
     * Reading any property of {@code undefined}
     * results in an error in JavaScript.
     */
    @Specialization(guards = "interopLibrary.isNull(target)", limit = "2")
    protected static Object readPropertyOfUndefined(
            Node node,
            @SuppressWarnings("unused") Object target,
            Object property,
            @CachedLibrary("target") @SuppressWarnings("unused") InteropLibrary interopLibrary,
            @CachedLibrary(limit = "2") DynamicObjectLibrary dynamicObjectLibrary,
            @Cached("currentLanguageContext().shapesAndPrototypes") @SuppressWarnings("truffle-neverdefault") ShapesAndPrototypes shapesAndPrototypes) {
        var typeError = new ErrorJavaScriptObject(
                "TypeError",
                "Cannot read properties of undefined (reading '" + property + "')",
                dynamicObjectLibrary,
                shapesAndPrototypes.rootShape,
                shapesAndPrototypes.errorPrototypes.typeErrorPrototype);
        throw new TypeScriptException(typeError, node);
    }

    /**
     * Accessing a property of anything that is not {@code undefined}
     * but doesn't have any members reads from the Object prototype.
     */
    @Fallback
    protected static Object readPropertyOfNonUndefinedWithoutMembers(@SuppressWarnings("unused") Object target,
            @SuppressWarnings("unused") Object property,
            @Cached("currentLanguageContext().shapesAndPrototypes.objectPrototype") ObjectPrototype objectPrototype,
            @CachedLibrary("objectPrototype") DynamicObjectLibrary dynamicObjectLibrary) {
        return dynamicObjectLibrary.getOrDefault(objectPrototype,
                TypeScriptTruffleStrings.toString(property), Undefined.INSTANCE);
    }
}

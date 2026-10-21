package nl.cge.lang.typescript.nodes.exprs.arrays;

import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.exprs.properties.CommonWritePropertyNode;
import nl.cge.lang.typescript.runtime.TypeScriptTruffleStrings;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * The Node representing writing array indexes
 * (like {@code a[1] = 3}).
 * Identical to the class with the same name from part 16.
 */
@NodeChild("arrayExpr")
@NodeChild("indexExpr")
@NodeChild("rvalueExpr")
@ImportStatic(TypeScriptTruffleStrings.class)
public abstract class ArrayIndexWriteExprNode extends TypeScriptExprNode {
    /**
     * A specialization for writing an integer index of an array,
     * in code like {@code [1, 2][1] = 3}.
     */
    @Specialization(guards = "arrayInteropLibrary.isArrayElementWritable(array, index)", limit = "2")
    protected Object writeIntIndexOfArray(
            Object array, int index, Object rvalue,
            @CachedLibrary("array") InteropLibrary arrayInteropLibrary) {
        try {
            arrayInteropLibrary.writeArrayElement(array, index, rvalue);
        } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
            throw new TypeScriptException(this, e.getMessage());
        }
        return rvalue;
    }

    /**
     * The cached version of the specialization for writing a string property of an object,
     * in code like {@code [1, 2]['abc'] = 3}.
     */
    @Specialization(guards = "equals(propertyName, cachedPropertyName, equalNode)", limit = "2")
    protected Object writeTruffleStringPropertyCached(
            Object target, TruffleString propertyName, Object rvalue,
            @Cached("propertyName") @SuppressWarnings("unused") TruffleString cachedPropertyName,
            @Cached @SuppressWarnings("unused") TruffleString.EqualNode equalNode,
            @Cached @Shared @SuppressWarnings("unused") TruffleString.ToJavaStringNode toJavaStringNode,
            @Cached("toJavaStringNode.execute(propertyName)") String javaStringPropertyName,
            @Cached(inline = true) @Shared CommonWritePropertyNode commonWritePropertyNode) {
        return commonWritePropertyNode.executeWriteProperty(this, target,
                javaStringPropertyName, rvalue);
    }

    /**
     * The uncached version of the specialization for writing a string property of an object,
     * in code like {@code [1, 2]['abc'] = 3}.
     */
    @Specialization(replaces = "writeTruffleStringPropertyCached")
    protected Object writeTruffleStringPropertyUncached(
            Object target, TruffleString propertyName, Object rvalue,
            @Cached @Shared TruffleString.ToJavaStringNode toJavaStringNode,
            @Cached(inline = true) @Shared CommonWritePropertyNode commonWritePropertyNode) {
        return commonWritePropertyNode.executeWriteProperty(this, target,
                toJavaStringNode.execute(propertyName), rvalue);
    }

    /**
     * A specialization for writing a non-string property of an object,
     * in code like {@code myObj[undefined] = "a"}, or {@code "a"[0] = 3}.
     * The index is converted to a string in that case.
     */
    @Fallback
    protected Object writeNonStringProperty(
            Object target, Object property, Object rvalue,
            @Cached(inline = true) @Shared CommonWritePropertyNode commonWritePropertyNode) {
        return commonWritePropertyNode.executeWriteProperty(this, target,
                TypeScriptTruffleStrings.toString(property), rvalue);
    }
}

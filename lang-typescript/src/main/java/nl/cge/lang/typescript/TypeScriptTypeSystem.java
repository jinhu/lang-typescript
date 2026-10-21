package nl.cge.lang.typescript;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

/**
 * The {@link TypeSystem} for TypeScript.
 * Identical to the class with the same name from part 16.
 */
@TypeSystem({
        boolean.class,
        int.class,
        double.class,
})
public abstract class TypeScriptTypeSystem {
    @ImplicitCast
    public static double castIntToDouble(int value) {
        return value;
    }
}

package nl.cge.lang.typescript.common;

import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import nl.cge.lang.typescript.runtime.ObjectPrototype;
import nl.cge.lang.typescript.TypeScriptLanguageContext;
import com.oracle.truffle.api.object.Shape;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class holds the {@link Shape}s and {@link ClassPrototypeObject}s
 * that are used by various TypeScript {@link com.oracle.truffle.api.nodes.Node}s.
 * They get access to this class through the
 * {@link TypeScriptLanguageContext TypeScript TruffleLanguage context}.
 * Identical to the class with the same name from part 16.
 */
public final class ShapesAndPrototypes {
    public final Shape rootShape;
    public final Shape arrayShape;
    public final ObjectPrototype objectPrototype;
    public final ClassPrototypeObject functionPrototype;
    public final ClassPrototypeObject arrayPrototype;
    public final ClassPrototypeObject stringPrototype;
    public final ErrorPrototypes errorPrototypes;
    public final Map<String, ClassPrototypeObject> allBuiltInClasses;

    public ShapesAndPrototypes(Shape rootShape, Shape arrayShape,
            ObjectPrototype objectPrototype, ClassPrototypeObject functionPrototype,
            ClassPrototypeObject arrayPrototype, ClassPrototypeObject stringPrototype,
            ErrorPrototypes errorPrototypes) {
        this.rootShape = rootShape;
        this.arrayShape = arrayShape;
        this.objectPrototype = objectPrototype;
        this.functionPrototype = functionPrototype;
        this.arrayPrototype = arrayPrototype;
        this.stringPrototype = stringPrototype;
        this.errorPrototypes = errorPrototypes;

        Map<String, ClassPrototypeObject> allBuiltInClasses = new HashMap<>();
        allBuiltInClasses.put("Object", objectPrototype);
        allBuiltInClasses.putAll(errorPrototypes.allBuiltInErrorClasses);
        this.allBuiltInClasses = Collections.unmodifiableMap(allBuiltInClasses);
    }
}

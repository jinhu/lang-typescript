package nl.cge.lang.typescript;

import nl.cge.lang.typescript.common.ErrorPrototypes;
import nl.cge.lang.typescript.common.ShapesAndPrototypes;
import nl.cge.lang.typescript.nodes.exprs.functions.ReadFunctionArgExprNode;
import nl.cge.lang.typescript.nodes.exprs.functions.built_in.AbsFunctionBodyExprNodeFactory;
import nl.cge.lang.typescript.nodes.exprs.functions.built_in.BuiltInFunctionBodyExprNode;
import nl.cge.lang.typescript.nodes.exprs.functions.built_in.PowFunctionBodyExprNodeFactory;
import nl.cge.lang.typescript.nodes.exprs.functions.built_in.methods.CharAtMethodBodyExprNodeFactory;
import nl.cge.lang.typescript.nodes.exprs.functions.built_in.methods.HasOwnPropertyMethodBodyExprNodeFactory;
import nl.cge.lang.typescript.nodes.exprs.literals.StringLiteralExprNode;
import nl.cge.lang.typescript.nodes.exprs.objects.ThisExprNode;
import nl.cge.lang.typescript.nodes.exprs.properties.PropertyWriteExprNodeGen;
import nl.cge.lang.typescript.nodes.root.BuiltInFuncRootNode;
import nl.cge.lang.typescript.nodes.root.StmtBlockRootNode;
import nl.cge.lang.typescript.nodes.stmts.ExprStmtNode;
import nl.cge.lang.typescript.nodes.stmts.blocks.BlockStmtNode;
import nl.cge.lang.typescript.parsing.TypeScriptTruffleParser;
import nl.cge.lang.typescript.parsing.ParsingResult;
import nl.cge.lang.typescript.runtime.ArrayObject;
import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import nl.cge.lang.typescript.runtime.FunctionObject;
import nl.cge.lang.typescript.runtime.GlobalScopeObject;
import nl.cge.lang.typescript.runtime.JavaScriptObject;
import nl.cge.lang.typescript.runtime.ObjectPrototype;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * The {@link TruffleLanguage} implementation for this part of the article series.
 * Identical to the class with the same name from part 16.
 */
@TruffleLanguage.Registration(id = "ts", name = "TypeScript")
@ProvidedTags({
        StandardTags.StatementTag.class, StandardTags.RootTag.class
})
public final class TypeScriptTruffleLanguage extends TruffleLanguage<TypeScriptLanguageContext> {
    private static final LanguageReference<TypeScriptTruffleLanguage> REF =
            LanguageReference.create(TypeScriptTruffleLanguage.class);

    /** Retrieve the current language instance for the given {@link Node}. */
    public static TypeScriptTruffleLanguage get(Node node) {
        return REF.get(node);
    }

    /** The root {@link Shape} for {@link ArrayObject} */
    private final Shape arrayShape = Shape.newBuilder().layout(ArrayObject.class, MethodHandles.lookup()).build();

    /**
     * The root {@link Shape} for {@link GlobalScopeObject}
     * and {@link ClassPrototypeObject}.
     */
    private final Shape rootShape = Shape.newBuilder().build();

    private final ObjectPrototype objectPrototype = new ObjectPrototype(this.rootShape);

    private final ClassPrototypeObject functionPrototype = new ClassPrototypeObject(this.rootShape,
            "Function", this.objectPrototype);

    private final ShapesAndPrototypes shapesAndPrototypes = this.createShapesAndPrototypes();

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        ParsingResult parsingResult = TypeScriptTruffleParser.parse(
                request.getSource(), this.shapesAndPrototypes);
        var programRootNode = new StmtBlockRootNode(this, parsingResult.topLevelFrameDescriptor,
                parsingResult.programStmtBlock, ":program", parsingResult.programSourceSection);
        return programRootNode.getCallTarget();
    }

    @Override
    protected TypeScriptLanguageContext createContext(Env env) {
        var objectLibrary = DynamicObjectLibrary.getUncached();
        return new TypeScriptLanguageContext(
                this.createGlobalScopeObject(objectLibrary),
                this.shapesAndPrototypes,
                // empty function, used for default constructors by SuperExprNode
                new FunctionObject(
                        this.rootShape,
                        this.functionPrototype,
                        new StmtBlockRootNode(
                                this,
                                FrameDescriptor.newBuilder().build(),
                                new BlockStmtNode(Collections.emptyList())
                        ).getCallTarget(),
                        0));
    }

    @Override
    protected Object getScope(TypeScriptLanguageContext context) {
        return context.globalScopeObject;
    }

    private DynamicObject createGlobalScopeObject(DynamicObjectLibrary objectLibrary) {
        var globalScopeObject = new GlobalScopeObject(this.rootShape);
        // the 0 flag indicates that these are variables, and can be reassigned
        objectLibrary.putConstant(globalScopeObject, "Math",
                this.createMathObject(objectLibrary), 0);

        // initialize the Object prototype
        objectLibrary.putConstant(this.objectPrototype, "hasOwnProperty",
                this.defineBuiltInMethod(HasOwnPropertyMethodBodyExprNodeFactory.getInstance()),
                0);

        // initialize the String prototype
        objectLibrary.putConstant(this.shapesAndPrototypes.stringPrototype, "charAt",
                this.defineBuiltInMethod(CharAtMethodBodyExprNodeFactory.getInstance()),
                0);

        // add all built-in class prototypes to the global scope
        for (var entry : this.shapesAndPrototypes.allBuiltInClasses.entrySet()) {
            objectLibrary.putConstant(globalScopeObject, entry.getKey(),
                    entry.getValue(), 0);
        }

        // add a constructor to all Error types
        for (Map.Entry<String, ClassPrototypeObject> entry :
                this.shapesAndPrototypes.errorPrototypes.allBuiltInErrorClasses.entrySet()) {
            objectLibrary.putConstant(entry.getValue(), "constructor",
                    // error subtype constructor
                    new FunctionObject(
                            this.rootShape,
                            this.functionPrototype,
                            new StmtBlockRootNode(
                                    this,
                                    FrameDescriptor.newBuilder().build(),
                                    new BlockStmtNode(List.of(
                                            // this.message = args[1];
                                            new ExprStmtNode(PropertyWriteExprNodeGen.create(
                                                    new ThisExprNode(),
                                                    new ReadFunctionArgExprNode(1, "message"),
                                                    "message"
                                            )),
                                            // this.name = <name>;
                                            new ExprStmtNode(PropertyWriteExprNodeGen.create(
                                                    new ThisExprNode(),
                                                    new StringLiteralExprNode(entry.getKey()),
                                                    "name"
                                            ))
                                    ))
                            ).getCallTarget(),
                            1),
                    0);
        }

        return globalScopeObject;
    }

    private Object createMathObject(DynamicObjectLibrary objectLibrary) {
        var mathPrototype = new ClassPrototypeObject(this.rootShape, "Math", this.objectPrototype);
        var mathObject = new JavaScriptObject(this.rootShape, mathPrototype);
        objectLibrary.putConstant(mathObject, "abs",
                this.defineBuiltInFunction(AbsFunctionBodyExprNodeFactory.getInstance()),
                0);
        objectLibrary.putConstant(mathObject, "pow",
                this.defineBuiltInFunction(PowFunctionBodyExprNodeFactory.getInstance()),
                0);
        return mathObject;
    }

    private ShapesAndPrototypes createShapesAndPrototypes() {
        var arrayPrototype = new ClassPrototypeObject(this.rootShape, "Array", this.objectPrototype);
        return new ShapesAndPrototypes(this.rootShape, this.arrayShape,
                this.objectPrototype, this.functionPrototype,
                arrayPrototype,
                new ClassPrototypeObject(this.rootShape, "String", this.objectPrototype),
                this.createErrorPrototypes());
    }

    private ErrorPrototypes createErrorPrototypes() {
        var errorPrototype = new ClassPrototypeObject(this.rootShape, "Error", this.objectPrototype);
        var typeErrorPrototype = new ClassPrototypeObject(this.rootShape, "TypeError", errorPrototype);
        return new ErrorPrototypes(errorPrototype, typeErrorPrototype);
    }

    private FunctionObject defineBuiltInFunction(NodeFactory<? extends BuiltInFunctionBodyExprNode> nodeFactory) {
        return new FunctionObject(this.rootShape, this.functionPrototype,
                this.createCallTarget(nodeFactory, /* offsetArguments */ true),
                nodeFactory.getExecutionSignature().size());
    }

    private FunctionObject defineBuiltInMethod(NodeFactory<? extends BuiltInFunctionBodyExprNode> nodeFactory) {
        return new FunctionObject(this.rootShape, this.functionPrototype,
                // built-in method implementation Nodes already have an argument for `this`,
                // so there's no need to offset the method arguments
                this.createCallTarget(nodeFactory, /* offsetArguments */ false),
                // we always add an extra argument for 'this' inside FunctionDispatchNode,
                // but built-in methods already have 'this' in their specializations -
                // for that reason, we make the FunctionObject have one argument less than the specializations take
                nodeFactory.getExecutionSignature().size() - 1);
    }

    private CallTarget createCallTarget(NodeFactory<? extends BuiltInFunctionBodyExprNode> nodeFactory,
            boolean offsetArguments) {
        int argumentCount = nodeFactory.getExecutionSignature().size();
        ReadFunctionArgExprNode[] functionArguments = IntStream.range(0, argumentCount)
                .mapToObj(i -> new ReadFunctionArgExprNode(offsetArguments ? i + 1 : i, "arg" + i))
                .toArray(ReadFunctionArgExprNode[]::new);
        var rootNode = new BuiltInFuncRootNode(this,
                nodeFactory.createNode((Object) functionArguments));
        return rootNode.getCallTarget();
    }
}

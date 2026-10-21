package nl.cge.lang.typescript.parsing;


import nl.cge.lang.typescript.common.DeclarationKind;
import nl.cge.lang.typescript.common.LocalVariableFrameSlotId;
import nl.cge.lang.typescript.common.ShapesAndPrototypes;
import nl.cge.lang.typescript.exceptions.*;
import nl.cge.lang.typescript.nodes.exprs.*;
import nl.cge.lang.typescript.nodes.exprs.arithmetic.*;
import nl.cge.lang.typescript.nodes.exprs.arrays.ArrayIndexReadExprNodeGen;
import nl.cge.lang.typescript.nodes.exprs.arrays.ArrayIndexWriteExprNodeGen;
import nl.cge.lang.typescript.nodes.exprs.arrays.ArrayLiteralExprNode;
import nl.cge.lang.typescript.nodes.exprs.comparisons.*;
import nl.cge.lang.typescript.nodes.exprs.frame.AbstractFrameGetNode;
import nl.cge.lang.typescript.nodes.exprs.frame.CurrentFrameGetNode;
import nl.cge.lang.typescript.nodes.exprs.frame.ParentFrameGetNode;
import nl.cge.lang.typescript.nodes.exprs.functions.*;
import nl.cge.lang.typescript.nodes.exprs.literals.*;
import nl.cge.lang.typescript.nodes.exprs.objects.*;
import nl.cge.lang.typescript.nodes.exprs.properties.PropertyReadExprNodeGen;
import nl.cge.lang.typescript.nodes.exprs.properties.PropertyWriteExprNodeGen;
import nl.cge.lang.typescript.nodes.exprs.variables.*;
import nl.cge.lang.typescript.nodes.stmts.*;
import nl.cge.lang.typescript.nodes.exprs.objects.*;
import nl.cge.lang.typescript.nodes.stmts.blocks.*;
import nl.cge.lang.typescript.nodes.stmts.controlflow.*;
import nl.cge.lang.typescript.nodes.stmts.exceptions.*;
import nl.cge.lang.typescript.nodes.stmts.loops.*;
import nl.cge.lang.typescript.nodes.stmts.variables.FuncDeclStmtNodeGen;
import nl.cge.lang.typescript.nodes.stmts.variables.GlobalVarDeclStmtNodeGen;
import nl.cge.lang.typescript.nodes.stmts.variables.NestedFuncDeclStmtNodeGen;
import nl.cge.lang.typescript.parser.TypeScriptLexer;
import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import nl.cge.lang.typescript.TypeScriptTruffleLanguage;
import nl.cge.lang.typescript.parser.TypeScriptParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * This is the class that parses the program and turns it into a Truffle AST.
 * It uses ANTLR to perform the actual parsing,
 * with the grammar defined in the {@code src/main/antlr/com/endoflineblog/truffle/part_17/parsing/antlr/TypeScriptParser.g4} file.
 * This class is invoked by the {@code TruffleLanguage} implementation for this part.
 *
 * @see #parse
 * @see TypeScriptTruffleLanguage
 */
public final class TypeScriptTruffleParser {
    public static ParsingResult parse(Source source, ShapesAndPrototypes shapesAndPrototypes) throws IOException {
        var TypeScriptTruffleParser = new TypeScriptTruffleParser(source, shapesAndPrototypes);
        return TypeScriptTruffleParser.parse();
    }

    private final Shape objectShape;
    private final Source source;
    private final TypeScriptParser parser;

    private enum ParserState { TOP_LEVEL, NESTED_SCOPE_IN_TOP_LEVEL, FUNC_DEF }

    /** Whether we're parsing a function definition. */
    private ParserState state;

    /**
     * The {@link FrameDescriptor} for either a given function definition,
     * or for any local variables in the statements of the top-level program.
     */
    private FrameDescriptor.Builder frameDescriptor;

    /**
     * The level of nesting of functions & methods.
     * Global-level statements are at 0,
     * top-level functions and class methods are at 1,
     * one-level nested functions are at 2, etc.
     */
    private int functionNestingLevel = 0;

    private static abstract class FrameMember {}
    private static final class FunctionArgument extends FrameMember {
        public final int argumentIndex;
        public final int nestingLevel;

        FunctionArgument(int argumentIndex, int nestingLevel) {
            this.argumentIndex = argumentIndex;
            this.nestingLevel = nestingLevel;
        }
    }
    private static final class LocalVariable extends FrameMember {
        public final int variableIndex;
        public final DeclarationKind declarationKind;
        public final int nestingLevel;

        LocalVariable(int variableIndex, DeclarationKind declarationKind, int nestingLevel) {
            this.variableIndex = variableIndex;
            this.declarationKind = declarationKind;
            this.nestingLevel = nestingLevel;
        }
    }
    private static final class ClassPrototypeMember extends FrameMember {
        public final ClassPrototypeObject classPrototypeObject;

        ClassPrototypeMember(ClassPrototypeObject classPrototypeObject) {
            this.classPrototypeObject = classPrototypeObject;
        }
    }

    /**
     * Map containing bindings for the function arguments and local variables when parsing function definitions
     * and nested scopes of the top-level scope.
     */
    private final Stack<Map<String, FrameMember>> localScopes;

    /**
     * The counter that makes it easy to generate unique variable names for local variables
     * (as their names can repeat in nested scopes).
     */
    private int localVariablesCounter;

    /**
     * The prototype of the class that we are currently parsing.
     * Needed to make {@code super} static, instead of dynamic,
     * like {@code this} is.
     */
    private ClassPrototypeObject currentClassPrototype;

    private TypeScriptTruffleParser(Source source, ShapesAndPrototypes shapesAndPrototypes) throws IOException {
        this.source = source;
        var lexer = new TypeScriptLexer(CharStreams.fromReader(source.getReader()));
        // remove the default console error listener
        lexer.removeErrorListeners();
        this.parser = new TypeScriptParser(new CommonTokenStream(lexer));
        // remove the default console error listener
        this.parser.removeErrorListeners();
        // throw an exception when a parsing error is encountered
        this.parser.setErrorHandler(new BailErrorStrategy());

        this.state = ParserState.TOP_LEVEL;
        this.frameDescriptor = FrameDescriptor.newBuilder();
        this.localScopes = new Stack<>();
        // we add a global scope, in which we store the class prototypes
        Map<String, FrameMember> classPrototypes = new HashMap<>();
        for (Map.Entry<String, ClassPrototypeObject> builtInClassEntry :
                shapesAndPrototypes.allBuiltInClasses.entrySet()) {
            classPrototypes.put(builtInClassEntry.getKey(),
                    new ClassPrototypeMember(builtInClassEntry.getValue()));
        }
        this.localScopes.push(classPrototypes);
        this.objectShape = shapesAndPrototypes.rootShape;
        this.localVariablesCounter = 0;
        this.currentClassPrototype = null;
    }

    public ParsingResult parse() {
        List<TypeScriptStmtNode> stmts = this.parseStmtsList(this.parser.start().stmt());
        SourceSection programSourceSection = this.source.createSection(0, this.source.getLength());
        boolean programBlock = true;
        return new ParsingResult(
                new BlockStmtNode(stmts, programBlock, programSourceSection),
                this.frameDescriptor.build(),

                // unit tests pass with this,
                // but the actual debugger has "double breakpoints" -
                // however, only in blocks in the main program!
                null

                // the unit test for breakpoint setting fails with this,
                // but the actual debugger works fine!
//                programSourceSection
        );
    }

    private List<TypeScriptStmtNode> parseStmtsList(List<TypeScriptParser.StmtContext> stmts) {
        var funcDecls = new ArrayList<TypeScriptStmtNode>();
        var nonFuncDeclStmts = new ArrayList<TypeScriptStmtNode>();
        for (TypeScriptParser.StmtContext stmt : stmts) {
            if (stmt instanceof TypeScriptParser.FuncDeclStmtContext) {
                funcDecls.add(this.parseFuncDeclStmt((TypeScriptParser.FuncDeclStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.ExprStmtContext) {
                nonFuncDeclStmts.add(this.parseExprStmt((TypeScriptParser.ExprStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.ClassDeclStmtContext) {
                nonFuncDeclStmts.add(this.parseClassDeclStmt((TypeScriptParser.ClassDeclStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.ReturnStmtContext) {
                nonFuncDeclStmts.add(this.parseReturnStmt((TypeScriptParser.ReturnStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.ThrowStmtContext) {
                nonFuncDeclStmts.add(this.parseThrowStmt((TypeScriptParser.ThrowStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.TryCatchStmtContext) {
                nonFuncDeclStmts.add(this.parseTryCatchStmt((TypeScriptParser.TryCatchStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.TryFinallyStmtContext) {
                nonFuncDeclStmts.add(this.parseTryFinallyStmt((TypeScriptParser.TryFinallyStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.IfStmtContext) {
                nonFuncDeclStmts.add(this.parseIfStmt((TypeScriptParser.IfStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.WhileStmtContext) {
                nonFuncDeclStmts.add(this.parseWhileStmt((TypeScriptParser.WhileStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.DoWhileStmtContext) {
                nonFuncDeclStmts.add(this.parseDoWhileStmt((TypeScriptParser.DoWhileStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.ForStmtContext) {
                nonFuncDeclStmts.add(this.parseForStmt((TypeScriptParser.ForStmtContext) stmt));
            } else if (stmt instanceof TypeScriptParser.BlockStmtContext) {
                nonFuncDeclStmts.add(this.parseStmtBlock(((TypeScriptParser.BlockStmtContext) stmt).stmt_block()));
            } else if (stmt instanceof TypeScriptParser.BreakStmtContext) {
                nonFuncDeclStmts.add(new BreakStmtNode(this.createSourceSection(stmt)));
            } else if (stmt instanceof TypeScriptParser.ContinueStmtContext) {
                nonFuncDeclStmts.add(new ContinueStmtNode(this.createSourceSection(stmt)));
            } else if (stmt instanceof TypeScriptParser.VarDeclStmtContext) {
                nonFuncDeclStmts.addAll(this.parseVarDeclStmt((TypeScriptParser.VarDeclStmtContext) stmt));
            }
        }

        // return the function declarations first, and then the remaining statements
        var result = new ArrayList<TypeScriptStmtNode>(funcDecls.size() + nonFuncDeclStmts.size());
        result.addAll(funcDecls);
        result.addAll(nonFuncDeclStmts);
        return result;
    }

    private ExprStmtNode parseExprStmt(TypeScriptParser.ExprStmtContext exprStmt) {
        return new ExprStmtNode(this.parseExpr1(exprStmt.expr1()), this.createSourceSection(exprStmt));
    }

    private ReturnStmtNode parseReturnStmt(TypeScriptParser.ReturnStmtContext returnStmt) {
        if (this.state != ParserState.FUNC_DEF) {
            throw new TypeScriptException("return statement is not allowed outside functions");
        }
        return new ReturnStmtNode(returnStmt.expr1() == null
                ? new UndefinedLiteralExprNode()
                : this.parseExpr1(returnStmt.expr1()),
                this.createSourceSection(returnStmt));
    }

    private ThrowStmtNode parseThrowStmt(TypeScriptParser.ThrowStmtContext throwStmt) {
        return ThrowStmtNodeGen.create(this.parseExpr1(throwStmt.expr1()),
                this.createSourceSection(throwStmt));
    }

    private TryStmtNode parseTryCatchStmt(TypeScriptParser.TryCatchStmtContext tryCatchStmt) {
        // parse the 'try' statement block
        BlockStmtNode tryBlockStmt = this.parseStmtBlock(tryCatchStmt.t);

        BlockStmtNode finallyBlockStmt = tryCatchStmt.f == null
                ? null
                : this.parseStmtBlock(tryCatchStmt.f);

        // add the 'catch' identifier as a local variable
        String exceptionVar = tryCatchStmt.ID().getText();
        var frameSlotId = new LocalVariableFrameSlotId(exceptionVar, ++this.localVariablesCounter);
        int frameSlot = this.frameDescriptor.addSlot(FrameSlotKind.Object, frameSlotId, DeclarationKind.LET);
        if (this.localScopes.peek().putIfAbsent(exceptionVar, new LocalVariable(frameSlot, DeclarationKind.LET, this.functionNestingLevel)) != null) {
            throw new TypeScriptException("Identifier '" + exceptionVar + "' has already been declared");
        }

        // parse the 'catch' statement block
        BlockStmtNode catchBlockStmt = this.parseStmtBlock(tryCatchStmt.c);

        return new TryStmtNode(tryBlockStmt, frameSlot, catchBlockStmt, finallyBlockStmt,
                this.createSourceSection(tryCatchStmt));
    }

    private TryStmtNode parseTryFinallyStmt(TypeScriptParser.TryFinallyStmtContext tryFinallyStmt) {
        // parse the 'try' statement block
        BlockStmtNode tryBlockStmt = this.parseStmtBlock(tryFinallyStmt.t);

        // parse the 'finally' statement block
        BlockStmtNode finallyBlockStmt = this.parseStmtBlock(tryFinallyStmt.f);

        return new TryStmtNode(tryBlockStmt, finallyBlockStmt, this.createSourceSection(tryFinallyStmt));
    }

    private IfStmtNode parseIfStmt(TypeScriptParser.IfStmtContext ifStmt) {
        return new IfStmtNode(
                this.parseExpr1(ifStmt.cond),
                this.parseStmt(ifStmt.then_stmt),
                this.parseStmt(ifStmt.else_stmt),
                this.createSourceSection(ifStmt));
    }

    private WhileStmtNode parseWhileStmt(TypeScriptParser.WhileStmtContext whileStmt) {
        return new WhileStmtNode(
                this.parseExpr1(whileStmt.cond),
                this.parseStmt(whileStmt.body),
                this.createSourceSection(whileStmt));
    }

    private DoWhileStmtNode parseDoWhileStmt(TypeScriptParser.DoWhileStmtContext doWhileStmt) {
        return new DoWhileStmtNode(
                this.parseExpr1(doWhileStmt.cond),
                this.parseStmtBlock(doWhileStmt.stmt_block()),
                this.createSourceSection(doWhileStmt));
    }

    private ForStmtNode parseForStmt(TypeScriptParser.ForStmtContext forStmt) {
        // a 'for' loop is its own scope
        ParserState previousParserState = this.state;

        if (this.state == ParserState.TOP_LEVEL) {
            this.state = ParserState.NESTED_SCOPE_IN_TOP_LEVEL;
        }
        this.localScopes.push(new HashMap<>());

        var ret = new ForStmtNode(
                this.parseStmt(forStmt.init),
                this.parseExpr1(forStmt.cond),
                this.parseExpr1(forStmt.updt),
                this.parseStmt(forStmt.body),
                this.createSourceSection(forStmt));

        // bring back the old state
        this.state = previousParserState;
        this.localScopes.pop();

        return ret;
    }

    private TypeScriptStmtNode parseStmt(TypeScriptParser.StmtContext stmt) {
        if (stmt == null) {
            return null;
        }
        List<TypeScriptStmtNode> parsedStmts = this.parseStmtsList(List.of(stmt));
        return parsedStmts.size() == 1
            ? parsedStmts.get(0)
            : new BlockStmtNode(parsedStmts, this.createSourceSection(stmt));
    }

    private BlockStmtNode parseStmtBlock(TypeScriptParser.Stmt_blockContext stmtBlock) {
        // save the current state of the parser (before entering the block)
        ParserState previousParserState = this.state;

        if (this.state == ParserState.TOP_LEVEL) {
            this.state = ParserState.NESTED_SCOPE_IN_TOP_LEVEL;
        }
        this.localScopes.push(new HashMap<>());

        // perform the parsing
        List<TypeScriptStmtNode> ret = this.parseStmtsList(stmtBlock.stmt());

        // bring back the old state
        this.state = previousParserState;
        this.localScopes.pop();

        return new BlockStmtNode(ret, this.createSourceSection(stmtBlock));
    }

    private List<TypeScriptStmtNode> parseVarDeclStmt(TypeScriptParser.VarDeclStmtContext varDeclStmt) {
        DeclarationKind declarationKind = DeclarationKind.fromToken(varDeclStmt.kind.getText());
        List<TypeScriptParser.BindingContext> varDeclBindings = varDeclStmt.binding();
        List<TypeScriptStmtNode> ret = new ArrayList<>(varDeclBindings.size());
        for (TypeScriptParser.BindingContext varBinding : varDeclBindings) {
            String variableId = varBinding.ID().getText();
            var bindingExpr = varBinding.expr1();
            TypeScriptExprNode initializerExpr;
            if (bindingExpr == null) {
                if (declarationKind == DeclarationKind.CONST) {
                    throw new TypeScriptException("Missing initializer in const declaration '" + variableId + "'");
                }
                // if a 'let' or 'var' declaration is missing an initializer,
                // it means it will be initialized with 'undefined'
                initializerExpr = new UndefinedLiteralExprNode();
            } else {
                initializerExpr = this.parseExpr1(bindingExpr);
            }

            if (this.state == ParserState.TOP_LEVEL) {
                // this is a global variable
                ret.add(GlobalVarDeclStmtNodeGen.create(
                        this.createSourceSection(varDeclStmt),
                        GlobalScopeObjectExprNodeGen.create(),
                        initializerExpr,
                        variableId,
                        declarationKind));
            } else {
                // this is a local variable (either of a function, or on the top-level)
                var frameSlotId = new LocalVariableFrameSlotId(variableId, ++this.localVariablesCounter);
                int frameSlot = this.frameDescriptor.addSlot(FrameSlotKind.Illegal, frameSlotId, declarationKind);
                if (this.localScopes.peek().putIfAbsent(variableId, new LocalVariable(frameSlot, declarationKind, this.functionNestingLevel)) != null) {
                    throw new TypeScriptException("Identifier '" + variableId + "' has already been declared");
                }
                LocalVarAssignmentExprNode assignmentExpr = LocalVarAssignmentExprNodeGen.create(
                        new CurrentFrameGetNode(), initializerExpr, variableId, frameSlot);
                ret.add(new ExprStmtNode(assignmentExpr,
                        this.createSourceSection(varDeclStmt), /* discardExpressionValue */ true));
            }
        }
        return ret;
    }

    private TypeScriptStmtNode parseFuncDeclStmt(TypeScriptParser.FuncDeclStmtContext funcDeclStmt) {
        return this.parseSubroutineDecl(funcDeclStmt.subroutine_decl(),
                GlobalScopeObjectExprNodeGen.create());
    }

    private TypeScriptStmtNode parseClassDeclStmt(TypeScriptParser.ClassDeclStmtContext classDeclStmt) {
        if (this.state == ParserState.FUNC_DEF) {
            // we do not allow nesting classes inside functions at the moment
            // (in theory, we could handle it by assigning the class object to a local variable,
            // but the additional complexity doesn't seem worth it)
            throw new TypeScriptException("classes nested in functions are not supported in TypeScript");
        }

        String className = classDeclStmt.cls.getText();
        String superClassName = classDeclStmt.spr_cls == null
                ? "Object"
                : classDeclStmt.spr_cls.getText();
        ClassPrototypeObject classPrototype;
        FrameMember superClassFrameMember = this.localScopes.firstElement().get(superClassName);
        if (superClassFrameMember instanceof ClassPrototypeMember) {
            ClassPrototypeObject superClassPrototype = ((ClassPrototypeMember) superClassFrameMember).classPrototypeObject;
            classPrototype = new ClassPrototypeObject(this.objectShape, className, superClassPrototype);
        } else {
            throw new TypeScriptException("class '" + className + "' extends unknown class '" + superClassName + "'");
        }
        this.localScopes.firstElement().put(className, new ClassPrototypeMember(classPrototype));
        this.currentClassPrototype = classPrototype;

        List<TypeScriptStmtNode> classMethods = new ArrayList<>();
        for (var classMember : classDeclStmt.class_member()) {
            classMethods.add(this.parseSubroutineDecl(classMember.subroutine_decl(),
                    new DynamicObjectReferenceExprNode(classPrototype)));
        }

        this.currentClassPrototype = null;
        return GlobalVarDeclStmtNodeGen.create(
                null,
                GlobalScopeObjectExprNodeGen.create(),
                new ClassDeclExprNode(classMethods, classPrototype),
                className, DeclarationKind.LET);
    }

    private TypeScriptStmtNode parseSubroutineDecl(TypeScriptParser.Subroutine_declContext subroutineDecl,
            TypeScriptExprNode containerObjectExpr) {
        String subroutineName = subroutineDecl.name.getText();
        boolean isNestedFunction = this.state == ParserState.FUNC_DEF;
        // make the default a characteristic value,
        // so we can easily find where it came from if we accidentally use it
        // (without assigning it the correct value first)
        int nestedFuncFrameSlot = -123;
        if (isNestedFunction) {
            // reserve a slot in the FrameDescriptor of the parent function for the nested function
            var frameSlotId = new LocalVariableFrameSlotId(subroutineName, ++this.localVariablesCounter);
            DeclarationKind declarationKind = DeclarationKind.LET;
            nestedFuncFrameSlot = this.frameDescriptor.addSlot(FrameSlotKind.Object, frameSlotId, declarationKind);
            if (this.localScopes.peek().putIfAbsent(subroutineName, new LocalVariable(nestedFuncFrameSlot, declarationKind, this.functionNestingLevel)) != null) {
                throw new TypeScriptException("Identifier '" + subroutineName + "' has already been declared");
            }
        }

        FuncDefExprNode funcDefExprNode = this.parseFuncDefExpr(
                subroutineDecl.args, subroutineDecl.stmt_block(),
                subroutineName, this.createSourceSection(subroutineDecl),
                isNestedFunction);

        return isNestedFunction
                ? NestedFuncDeclStmtNodeGen.create(
                        funcDefExprNode,
                        nestedFuncFrameSlot)
                : FuncDeclStmtNodeGen.create(
                        containerObjectExpr,
                        funcDefExprNode,
                        subroutineName);
    }

    private TypeScriptExprNode parseExpr1(TypeScriptParser.Expr1Context expr1) {
        if (expr1 == null) {
            return null;
        }
        if (expr1 instanceof TypeScriptParser.AssignmentExpr1Context) {
            return parseAssignmentExpr((TypeScriptParser.AssignmentExpr1Context) expr1);
        } else if (expr1 instanceof TypeScriptParser.PropertyWriteExpr1Context) {
            return this.parsePropertyWriteExpr((TypeScriptParser.PropertyWriteExpr1Context) expr1);
        } else if (expr1 instanceof TypeScriptParser.ArrayIndexWriteExpr1Context) {
            return this.parseArrayIndexWriteExpr((TypeScriptParser.ArrayIndexWriteExpr1Context) expr1);
        } else if (expr1 instanceof TypeScriptParser.LambdaExpr1Context) {
            return this.parseLambdaExpr((TypeScriptParser.LambdaExpr1Context) expr1);
        } else {
            return parseExpr2(((TypeScriptParser.PrecedenceTwoExpr1Context) expr1).expr2());
        }
    }

    private FuncDefExprNode parseLambdaExpr(TypeScriptParser.LambdaExpr1Context lambdaExpr) {
        return this.parseFuncDefExpr(lambdaExpr.args, lambdaExpr.stmt_block(), null,
                this.createSourceSection(lambdaExpr), /* isClosure */ true);
    }

    private FuncDefExprNode parseFuncDefExpr(
        TypeScriptParser.Func_argsContext args, TypeScriptParser.Stmt_blockContext stmtBlock,
        String funcName, SourceSection sourceSection, boolean isClosure
    ) {
        // save the current state of the parser (before entering the function)
        FrameDescriptor.Builder previousFrameDescriptor = this.frameDescriptor;
        ParserState previousParserState = this.state;

        // initialize the new state
        this.frameDescriptor = FrameDescriptor.newBuilder();
        this.state = ParserState.FUNC_DEF;
        this.functionNestingLevel++;

        var localVariables = new HashMap<String, FrameMember>();
        // add each parameter to the map, with the correct index
        List<TerminalNode> funcArgs = args.ID();
        int argumentCount = funcArgs.size();
        // all arguments need to be offset by 1 because of 'this',
        // and closures need offset by 2, to account for the parent frame
        int offset = isClosure ? 2 : 1;
        // first, initialize the locals with function arguments
        for (int i = 0; i < argumentCount; i++) {
            localVariables.put(funcArgs.get(i).getText(), new FunctionArgument(i + offset, this.functionNestingLevel));
        }
        this.localScopes.push(localVariables);

        // parse the statements in the function definition
        List<TypeScriptStmtNode> funcStmts = this.parseStmtsList(stmtBlock.stmt());

        FrameDescriptor frameDescriptor = this.frameDescriptor.build();
        // bring back the old state
        this.frameDescriptor = previousFrameDescriptor;
        this.state = previousParserState;
        this.localScopes.pop();
        this.functionNestingLevel--;

        return new FuncDefExprNode(
                frameDescriptor,
                new UserFuncBodyStmtNode(funcStmts, sourceSection),
                funcName, argumentCount, isClosure);
    }

    private TypeScriptExprNode parseAssignmentExpr(TypeScriptParser.AssignmentExpr1Context assignmentExpr) {
        String variableId = assignmentExpr.ID().getText();
        FrameMember frameMember = this.findFrameMember(variableId);
        TypeScriptExprNode initializerExpr = this.parseExpr1(assignmentExpr.expr1());
        if (frameMember == null || frameMember instanceof ClassPrototypeMember) {
            return GlobalVarAssignmentExprNodeGen.create(GlobalScopeObjectExprNodeGen.create(), initializerExpr, variableId);
        } else if (frameMember instanceof FunctionArgument) {
            var functionArgument = (FunctionArgument) frameMember;
            AbstractFrameGetNode currentOrParentFrameGetNode = this.establishCurrentOrParentGetFrameNode(functionArgument.nestingLevel);
            return new WriteFunctionArgExprNode(currentOrParentFrameGetNode, initializerExpr, functionArgument.argumentIndex);
        } else {
            var localVariable = (LocalVariable) frameMember;
            if (localVariable.declarationKind == DeclarationKind.CONST) {
                throw new TypeScriptException("Assignment to constant variable '" + variableId + "'");
            }
            AbstractFrameGetNode currentOrParentFrameGetNode = this.establishCurrentOrParentGetFrameNode(localVariable.nestingLevel);
            return LocalVarAssignmentExprNodeGen.create(
                    currentOrParentFrameGetNode, initializerExpr, variableId, localVariable.variableIndex);
        }
    }

    private TypeScriptExprNode parsePropertyWriteExpr(TypeScriptParser.PropertyWriteExpr1Context propertyWriteExpr) {
        return PropertyWriteExprNodeGen.create(
                this.parseExpr5(propertyWriteExpr.object),
                this.parseExpr1(propertyWriteExpr.rvalue),
                propertyWriteExpr.ID().getText());
    }

    private TypeScriptExprNode parseArrayIndexWriteExpr(TypeScriptParser.ArrayIndexWriteExpr1Context arrayIndexWriteExpr) {
        return ArrayIndexWriteExprNodeGen.create(
                this.parseExpr5(arrayIndexWriteExpr.arr),
                this.parseExpr1(arrayIndexWriteExpr.index),
                this.parseExpr1(arrayIndexWriteExpr.rvalue));
    }

    private TypeScriptExprNode parseExpr2(TypeScriptParser.Expr2Context expr2) {
        return expr2 instanceof TypeScriptParser.EqNotEqExpr2Context
                ? this.parseEqNotEqExpression(((TypeScriptParser.EqNotEqExpr2Context) expr2))
                : this.parseExpr3(((TypeScriptParser.PrecedenceThreeExpr2Context) expr2).expr3());
    }

    private TypeScriptExprNode parseEqNotEqExpression(TypeScriptParser.EqNotEqExpr2Context eqNotEqExpr) {
        TypeScriptExprNode leftSide = this.parseExpr2(eqNotEqExpr.expr2());
        TypeScriptExprNode rightSide = this.parseExpr3(eqNotEqExpr.expr3());
        return "===".equals(eqNotEqExpr.c.getText())
                ? EqualityExprNodeGen.create(leftSide, rightSide)
                : InequalityExprNodeGen.create(leftSide, rightSide);
    }

    private TypeScriptExprNode parseExpr3(TypeScriptParser.Expr3Context expr3) {
        if (expr3 instanceof TypeScriptParser.ComparisonExpr3Context) {
            return this.parseComparisonExpr(((TypeScriptParser.ComparisonExpr3Context) expr3));
        } else {
            return this.parseExpr4(((TypeScriptParser.PrecedenceFourExpr3Context) expr3).expr4());
        }
    }

    private TypeScriptExprNode parseComparisonExpr(TypeScriptParser.ComparisonExpr3Context comparisonExpr) {
        TypeScriptExprNode leftSide = this.parseExpr3(comparisonExpr.expr3());
        TypeScriptExprNode rightSide = this.parseExpr4(comparisonExpr.expr4());
        switch (comparisonExpr.c.getText()) {
            case  "<": return LesserExprNodeGen.create(leftSide, rightSide);
            case "<=": return LesserOrEqualExprNodeGen.create(leftSide, rightSide);
            case  ">": return GreaterExprNodeGen.create(leftSide, rightSide);
            default:
            case ">=": return GreaterOrEqualExprNodeGen.create(leftSide, rightSide);
        }
    }

    private TypeScriptExprNode parseExpr4(TypeScriptParser.Expr4Context expr4) {
        if (expr4 instanceof TypeScriptParser.AddSubtractExpr4Context) {
            return this.parseAdditionSubtractionExpr((TypeScriptParser.AddSubtractExpr4Context) expr4);
        } else if (expr4 instanceof TypeScriptParser.UnaryMinusExpr4Context) {
            return parseUnaryMinusExpr((TypeScriptParser.UnaryMinusExpr4Context) expr4);
        } else {
            return parseExpr5(((TypeScriptParser.PrecedenceFiveExpr4Context) expr4).expr5());
        }
    }

    private TypeScriptExprNode parseAdditionSubtractionExpr(TypeScriptParser.AddSubtractExpr4Context addSubtractExpr) {
        TypeScriptExprNode leftSide = this.parseExpr4(addSubtractExpr.left);
        TypeScriptExprNode rightSide = this.parseExpr5(addSubtractExpr.right);
        switch (addSubtractExpr.o.getText()) {
            case "-":
                return SubtractionExprNodeGen.create(leftSide, rightSide);
            case "+":
            default:
                return AdditionExprNodeGen.create(leftSide, rightSide);
        }
    }

    private NegationExprNode parseUnaryMinusExpr(TypeScriptParser.UnaryMinusExpr4Context unaryMinusExpr) {
        return NegationExprNodeGen.create(parseExpr5(unaryMinusExpr.expr5()));
    }

    private TypeScriptExprNode parseExpr5(TypeScriptParser.Expr5Context expr5) {
        if (expr5 instanceof TypeScriptParser.PropertyReadExpr5Context) {
            return this.parsePropertyReadExpr((TypeScriptParser.PropertyReadExpr5Context) expr5);
        } else if (expr5 instanceof TypeScriptParser.ArrayIndexReadExpr5Context) {
            return this.parseArrayIndexReadExpr((TypeScriptParser.ArrayIndexReadExpr5Context) expr5);
        } else if (expr5 instanceof TypeScriptParser.CallExpr5Context) {
            return parseCallExpr((TypeScriptParser.CallExpr5Context) expr5);
        } else {
            return this.parseExpr6(((TypeScriptParser.PrecedenceSixExpr5Context) expr5).expr6());
        }
    }

    private TypeScriptExprNode parseExpr6(TypeScriptParser.Expr6Context expr6) {
        if (expr6 instanceof TypeScriptParser.LiteralExpr6Context) {
            return this.parseLiteralExpr((TypeScriptParser.LiteralExpr6Context) expr6);
        } else if (expr6 instanceof TypeScriptParser.ThisExpr6Context) {
            return new ThisExprNode();
        } else if (expr6 instanceof TypeScriptParser.SuperExpr6Context) {
            return this.parseSuperExpr();
        } else if (expr6 instanceof TypeScriptParser.ReferenceExpr6Context) {
            return this.parseReference(((TypeScriptParser.ReferenceExpr6Context) expr6).ID().getText());
        } else if (expr6 instanceof TypeScriptParser.ArrayLiteralExpr6Context) {
            return this.parseArrayLiteralExpr((TypeScriptParser.ArrayLiteralExpr6Context) expr6);
        } else if (expr6 instanceof TypeScriptParser.NewExpr6Context) {
            return this.parseNewExpr((TypeScriptParser.NewExpr6Context) expr6);
        } else {
            return this.parseExpr1(((TypeScriptParser.PrecedenceOneExpr6Context) expr6).expr1());
        }
    }

    private TypeScriptExprNode parseLiteralExpr(TypeScriptParser.LiteralExpr6Context literalExpr) {
        TerminalNode intTerminal = literalExpr.literal().INT();
        if (intTerminal != null) {
            return parseIntLiteral(intTerminal.getText());
        }
        TerminalNode doubleTerminal = literalExpr.literal().DOUBLE();
        if (doubleTerminal != null) {
            return parseDoubleLiteral(doubleTerminal.getText());
        }
        TypeScriptParser.Bool_literalContext boolLiteral = literalExpr.literal().bool_literal();
        if (boolLiteral != null) {
            return new BoolLiteralExprNode("true".equals(boolLiteral.getText()));
        }
        TypeScriptParser.String_literalContext stringTerminal = literalExpr.literal().string_literal();
        if (stringTerminal != null) {
            String stringLiteral = stringTerminal.getText();
            // remove the quotes delineating the string literal,
            // and unescape the string (meaning, turn \' into ', etc.)
            return new StringLiteralExprNode(StringEscapeUtils.unescapeJson(
                    stringLiteral.substring(1, stringLiteral.length() - 1)));
        }
        return new UndefinedLiteralExprNode();
    }

    private TypeScriptExprNode parseSuperExpr() {
        if (this.currentClassPrototype == null) {
            throw new TypeScriptException("'super' is only available in class declarations");
        }
        return new SuperExprNode(this.currentClassPrototype);
    }

    private TypeScriptExprNode parseReference(String variableId) {
        FrameMember frameMember = this.findFrameMember(variableId);
        if (frameMember == null || frameMember instanceof ClassPrototypeMember) {
            // we know for sure this is a reference to a global variable
            return GlobalVarReferenceExprNodeGen.create(GlobalScopeObjectExprNodeGen.create(), variableId);
        } else if (frameMember instanceof FunctionArgument) {
            var functionArgument = (FunctionArgument) frameMember;
            AbstractFrameGetNode currentOrParentGetFrameNode = this.establishCurrentOrParentGetFrameNode(functionArgument.nestingLevel);
            return new ReadFunctionArgExprNode(currentOrParentGetFrameNode, functionArgument.argumentIndex, variableId);
        } else {
            var localVariable = (LocalVariable) frameMember;
            AbstractFrameGetNode currentOrParentFrameGetNode = this.establishCurrentOrParentGetFrameNode(localVariable.nestingLevel);
            return LocalVarReferenceExprNodeGen.create(currentOrParentFrameGetNode, localVariable.variableIndex);
        }
    }

    private TypeScriptExprNode parsePropertyReadExpr(TypeScriptParser.PropertyReadExpr5Context propertyReadExpr) {
        return PropertyReadExprNodeGen.create(
                this.parseExpr5(propertyReadExpr.expr5()),
                propertyReadExpr.ID().getText());
    }

    private TypeScriptExprNode parseNewExpr(TypeScriptParser.NewExpr6Context newExpr) {
        return NewExprNodeGen.create(
                this.parseExpr6(newExpr.constr),
                newExpr.expr1().stream()
                        .map(this::parseExpr1)
                        .collect(Collectors.toList()));
    }

    private ArrayLiteralExprNode parseArrayLiteralExpr(TypeScriptParser.ArrayLiteralExpr6Context arrayLiteralExpr) {
        return new ArrayLiteralExprNode(arrayLiteralExpr.expr1().stream()
                .map(this::parseExpr1)
                .collect(Collectors.toList()));
    }

    private TypeScriptExprNode parseArrayIndexReadExpr(TypeScriptParser.ArrayIndexReadExpr5Context arrayIndexReadExpr) {
        return ArrayIndexReadExprNodeGen.create(
                this.parseExpr5(arrayIndexReadExpr.arr),
                this.parseExpr1(arrayIndexReadExpr.index));
    }

    private FunctionCallExprNode parseCallExpr(TypeScriptParser.CallExpr5Context callExpr) {
        return new FunctionCallExprNode(
                parseExpr5(callExpr.expr5()),
                callExpr.expr1().stream()
                        .map(this::parseExpr1)
                        .collect(Collectors.toList()));
    }

    private TypeScriptExprNode parseIntLiteral(String text) {
        try {
            return new IntLiteralExprNode(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            // it's possible that the integer literal is too big to fit in a 32-bit Java `int` -
            // in that case, fall back to a double literal
            return parseDoubleLiteral(text);
        }
    }

    private DoubleLiteralExprNode parseDoubleLiteral(String text) {
        return new DoubleLiteralExprNode(Double.parseDouble(text));
    }

    private AbstractFrameGetNode establishCurrentOrParentGetFrameNode(int referenceNestingLevel) {
        AbstractFrameGetNode currentOrParentFrameGetNode = new CurrentFrameGetNode();
        for (int i = 0; i < this.functionNestingLevel - referenceNestingLevel; i++) {
            currentOrParentFrameGetNode = new ParentFrameGetNode(currentOrParentFrameGetNode);
        }
        return currentOrParentFrameGetNode;
    }

    private FrameMember findFrameMember(String memberName) {
        for (var iter = this.localScopes.listIterator(this.localScopes.size()); iter.hasPrevious();) {
            FrameMember ret = iter.previous().get(memberName);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private SourceSection createSourceSection(ParserRuleContext parseElement) {
        return this.source.createSection(
                parseElement.start.getLine(), parseElement.start.getCharPositionInLine() + 1,
                parseElement.stop.getLine(), parseElement.stop.getCharPositionInLine() + 1);
    }
}

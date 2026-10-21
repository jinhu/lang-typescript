package nl.cge.lang.typescript.nodes.exprs.functions.built_in;

import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.nodes.exprs.functions.ReadFunctionArgExprNode;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;

/**
 * The common ancestor for Nodes that represent the implementations of
 * built-in JavaScript functions and methods.
 * Identical to the class with the same name from part 16.
 */
@NodeChild(value = "arguments", type = ReadFunctionArgExprNode[].class)
@GenerateNodeFactory
public abstract class BuiltInFunctionBodyExprNode extends TypeScriptExprNode {
}

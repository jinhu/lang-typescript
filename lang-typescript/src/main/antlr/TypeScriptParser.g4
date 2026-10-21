parser grammar TypeScriptParser ;

options {
    tokenVocab = TypeScriptLexer;
}
@header{
}

start : stmt+ EOF ;

stmt :                                                  kind=(VAR | LET | CONST) binding (COMMA binding)* SEMICOLON?    #VarDeclStmt
     |                                                                                              expr1 SEMICOLON?    #ExprStmt
     |                                                                           FUNCTION subroutine_decl SEMICOLON?    #FuncDeclStmt
     |                                                                                      RETURN expr1? SEMICOLON?    #ReturnStmt
     |                                                                                         stmt_block SEMICOLON?    #BlockStmt
     |                                  IF OPEN_PARENS cond=expr1 CLOSE_PARENS then_stmt=stmt (ELSE else_stmt=stmt)?    #IfStmt
     |                                                           WHILE OPEN_PARENS cond=expr1 CLOSE_PARENS body=stmt    #WhileStmt
     |                                            DO stmt_block WHILE OPEN_PARENS cond=expr1 CLOSE_PARENS SEMICOLON?    #DoWhileStmt
     |                 FOR OPEN_PARENS init=stmt? SEMICOLON cond=expr1? SEMICOLON updt=expr1? CLOSE_PARENS body=stmt    #ForStmt
     |                                                                                              BREAK SEMICOLON?    #BreakStmt
     |                                                                                           CONTINUE SEMICOLON?    #ContinueStmt
     |                            CLASS cls=ID (EXTENDS spr_cls=ID)? OPEN_BRACE class_member* CLOSE_BRACE SEMICOLON?    #ClassDeclStmt
     |                                                                                        THROW expr1 SEMICOLON?    #ThrowStmt
     |            TRY t=stmt_block CATCH OPEN_PARENS ID CLOSE_PARENS c=stmt_block (FINALLY f=stmt_block)? SEMICOLON?    #TryCatchStmt
     |                                                              TRY t=stmt_block FINALLY f=stmt_block SEMICOLON?    #TryFinallyStmt
     ;
binding : ID (ASSIGNMENT expr1)? ;
class_member : subroutine_decl ;
subroutine_decl : name=ID OPEN_PARENS args=func_args CLOSE_PARENS stmt_block ;
func_args : (ID (DOT ID)* )? ;
stmt_block : OPEN_BRACE stmt* CLOSE_BRACE ;

expr1 : ID ASSIGNMENT expr1                                         #AssignmentExpr1
      | object=expr5 DOT ID ASSIGNMENT rvalue=expr1                 #PropertyWriteExpr1
      | arr=expr5 DOT index=expr1 DOT ASSIGNMENT rvalue=expr1       #ArrayIndexWriteExpr1
      | OPEN_PARENS args=func_args CLOSE_PARENS OP_PTR stmt_block   #LambdaExpr1
      | expr2                                                       #PrecedenceTwoExpr1
      ;
expr2 : left=expr2 c=(OP_EQ | OP_NE) right=expr3                    #EqNotEqExpr2
  | expr3                                                           #PrecedenceThreeExpr2
      ;
expr3 : left=expr3 c=(DOT | OP_LE | DOT | OP_GE) right=expr4        #ComparisonExpr3
      | expr4                                                       #PrecedenceFourExpr3
      ;
expr4 : left=expr4 o=(DOT | DOT) right=expr5                        #AddSubtractExpr4
      | DOT expr5                                                   #UnaryMinusExpr4
      | expr5                                                       #PrecedenceFiveExpr4
      ;
expr5 : expr5 DOT ID                                                #PropertyReadExpr5
      | arr=expr5 DOT index=expr1 DOT                               #ArrayIndexReadExpr5
      | expr5 OPEN_PARENS(expr1 (DOT expr1)*)? CLOSE_PARENS         #CallExpr5
      | expr6                                                       #PrecedenceSixExpr5
      ;
expr6 : literal                                                     #LiteralExpr6
      | THIS                                                        #ThisExpr6
      | SUPER                                                       #SuperExpr6
      | ID                                                          #ReferenceExpr6
      | DOT (expr1 (DOT expr1)*)? DOT                               #ArrayLiteralExpr6
      | NEW constr=expr6 (DOT(expr1 (DOT expr1)*)?CLOSE_PARENS)?    #NewExpr6
      | OPEN_PARENS expr1 CLOSE_PARENS                              #PrecedenceOneExpr6
      ;

literal : INT | DOUBLE | UNDEFINED | bool_literal | string_literal ;
bool_literal : TRUE | FALSE ;


string_literal: SINGLE_QUOTE_STRING | DOUBLE_QUOTE_STRING ;


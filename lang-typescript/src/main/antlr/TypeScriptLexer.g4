lexer grammar TypeScriptLexer ;

channels {
    COMMENTS_CHANNEL
}

// Literals
fragment DIGIT : [0-9] ;
DOUBLE : DIGIT+ '.' DIGIT+ ;
INT : DIGIT+ ;

// Keywords
VAR : 'var' ;
LET : 'let' ;
CONST : 'const' ;
FUNCTION : 'function' ;
RETURN : 'return' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
DO : 'do' ;
FOR : 'for' ;
BREAK : 'break' ;
CONTINUE : 'continue' ;
CLASS : 'class' ;
EXTENDS : 'extends' ;
THIS : 'this' ;
SUPER : 'super' ;
NEW : 'new' ;
THROW : 'throw' ;
TRY : 'try' ;
CATCH : 'catch' ;
FINALLY : 'finally' ;
UNDEFINED : 'undefined' ;
TRUE : 'true' ;
FALSE : 'false' ;

fragment LETTER : [a-zA-Z$_] ;
ID : LETTER (LETTER | DIGIT)* ;

// see https://stackoverflow.com/questions/24557953/handling-string-literals-which-end-in-an-escaped-quote-in-antlr4
// for details
SINGLE_QUOTE_STRING : '\'' (~[\\'\r\n] | '\\' ~[\r\n])* '\'' ;
DOUBLE_QUOTE_STRING : '"'  (~[\\"\r\n] | '\\' ~[\r\n])* '"' ;

// Punctuators and operators
OPEN_BRACE : '{' ;
CLOSE_BRACE : '}' ;
OPEN_BRACKET : '[' ;
CLOSE_BRACKET : ']' ;
OPEN_PARENS : '(' ;
CLOSE_PARENS : ')' ;
DOT : '.' ;
COMMA : ',' ;
SEMICOLON : ';' ;
ASSIGNMENT : '=' ;
OP_PTR : '=>' ;
OP_EQ : '===' ;
OP_NE : '!==' ;
LT : '<' ;
OP_LE : '<=' ;
GT : '>' ;
OP_GE : '>=' ;
PLUS : '+' ;
MINUS : '-' ;

// Whitespace and comments
WS : (' ' | '\r' | '\t' | '\n' | '\f')+ -> channel(HIDDEN) ;
fragment InputCharacter : ~[\r\n\u0085\u2028\u2029] ;
SINGLE_LINE_DOC_COMMENT : '///' InputCharacter* -> channel(COMMENTS_CHANNEL) ;
EMPTY_DELIMITED_DOC_COMMENT : '/***/' -> channel(COMMENTS_CHANNEL) ;
DELIMITED_DOC_COMMENT : '/**' ~'/' .*? '*/' -> channel(COMMENTS_CHANNEL) ;
SINGLE_LINE_COMMENT : '//' InputCharacter* -> channel(COMMENTS_CHANNEL) ;
DELIMITED_COMMENT : '/*' .*? '*/' -> channel(COMMENTS_CHANNEL) ;

grammar Mill; // Mill Invokes Little Labor

@header {
package com.abcdabcd987.compiler2016.Parser;
}

program
    :   (   classDeclaration
        |   functionDeclaration
        |   variableDeclaration
        )* EOF
    ;

//------ Statement
statement
    :   blockStatement
    |   expressionStatement
    |   selectionStatement
    |   iterationStatement
    |   jumpStatement
    ;

blockStatement
    :  '{' blockItem* '}'
    ;

blockItem
    : variableDeclaration
    | statement
    ;

expressionStatement
    :   expression? ';'
    ;

selectionStatement
    :   'if' '(' expression ')' statement ('else' statement)?
    ;

iterationStatement
    :   'while' '(' expression ')' statement
    |   'for' '(' expression? ';' expression? ';' expression? ')' statement
    //|   'for' '(' declaration expression? ';' expression? ')' statement
    ;

jumpStatement
    :   'continue' ';'
    |   'break' ';'
    |   'return' expression? ';'
    ;

//------ Declaration
variableDeclarator
    :   typeSpecifier Identifier
    ;

typeSpecifier
    :   'int' | 'bool' | 'string'
    |   Identifier
    |   typeSpecifier '[' ']'
    ;

variableDeclaration
    :   variableDeclarator ('=' expression) ';'
    ;

classDeclaration
    :   'class' Identifier '{' memberDeclaration* '}'
    ;

memberDeclaration
    :   variableDeclarator ';'
    ;

functionDeclaration
    :   (variableDeclarator | 'void') '(' parameterDeclarationList? ')' blockStatement
    ;

parameterDeclarationList
    :   parameterDeclaration (',' parameterDeclaration)*
    ;

parameterDeclaration
    :   variableDeclarator
    ;

//------ Expression: http://en.cppreference.com/w/cpp/language/operator_precedence
expression
    :   expression op=('++' | '--')                  # PostfixIncDec    // Precedence 1
    |   expression '(' parameterList? ')'            # FunctionCall
    |   expression '[' expression ']'                # Subscript
    |   expression '.' Identifier                    # MemberAccess

    |   <assoc=right> op=('++'|'--') expression      # PrefixIncDec     // Precedence 2
    |   <assoc=right> op=('+' | '-') expression      # UnaryPlusMinus
    |   <assoc=right> op=('!' | '~') expression      # BoolBitNot
    |   <assoc=right> op='new' expression            # New

    |   expression op=('*' | '/' | '%') expression   # MulDivMod        // Precedence 3
    |   expression op=('+' | '-') expression         # AddSub           // Precedence 4
    |   expression op=('<<'|'>>') expression         # LefRigShift      // Precedence 5
    |   expression op=('<' | '>') expression         # LtRt             // Precedence 6
    |   expression op=('<='|'>=') expression         # LeqReq
    |   expression op=('=='|'!=') expression         # EqNeq            // Precedence 7
    |   expression op='&' expression                 # BitAnd           // Precedence 8
    |   expression op='^' expression                 # BitXor           // Precedence 9
    |   expression op='|' expression                 # BitOr            // Precedence 10
    |   expression op='&&' expression                # BoolAnd          // Precedence 11
    |   expression op='||' expression                # BoolOr           // Precedence 12

    |   expression op='=' expression                 # Assign           // Precedence 14

    |   Identifier                                   # Identifier
    |   Constant                                     # Literal
    |   '(' expression ')'                           # SubExpression
    ;

parameterList
    :   parameter (',' parameter)*
    ;

parameter
    :   expression
    ;

//------ Reserved Keywords
Bool : 'bool';
Int : 'int';
String : 'string';
Null : 'null';
Void : 'void';
True : 'true';
False : 'false';
If : 'if';
For : 'for';
While : 'while';
Break : 'break';
Continue : 'continue';
Return : 'return';
New : 'new';
Class : 'class';

//------ Symbol
LParen : '(';
RParen : ')';
LBracket : '[';
RBracket : ']';
LBrace : '{';
RBrace : '}';

Less : '<';
LessEqual : '<=';
Greater : '>';
GreaterEqual : '>=';
LeftShift : '<<';
RightShift : '>>';

Plus : '+';
PlusPlus : '++';
Minus : '-';
MinusMinus : '--';
Star : '*';
Div : '/';
Mod : '%';

And : '&';
Or : '|';
AndAnd : '&&';
OrOr : '||';
Caret : '^';
Not : '!';
Tilde : '~';

Question : '?';
Colon : ':';
Semi : ';';
Comma : ',';

Assign : '=';

Equal : '==';
NotEqual : '!=';

Dot : '.';

//------ Identifier
Identifier
    :   IdentifierNondigit ( IdentifierNondigit | Digit )*
    ;

fragment
IdentifierNondigit
    :   [a-zA-Z_]
    ;

fragment
Digit
    :   [0-9]
    ;

//------ Constant

Constant
    :   IntegerConstant
    |   CharacterConstant
    |   StringLiteral
    ;

fragment
IntegerConstant
    :   NonzeroDigit Digit*
    |   '0'
    ;

fragment
NonzeroDigit
    :   [1-9]
    ;

fragment
CharacterConstant
    :   '\'' CCharSequence '\''
    ;

fragment
CCharSequence
    :   CChar+
    ;

fragment
CChar
    :   ~['\\\r\n]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   SimpleEscapeSequence
    ;

fragment
SimpleEscapeSequence
    :   '\\' ['"?abfnrtv\\]
    ;

StringLiteral
    :   '"' SCharSequence? '"'
    ;

fragment
SCharSequence
    :   SChar+
    ;

fragment
SChar
    :   ~["\\\r\n]
    |   EscapeSequence
    ;

Whitespace
    :   [ \t]+ -> skip
    ;

Newline
    :   '\r'? '\n' -> skip
    ;

BlockComment
    :   '/*' .*? '*/' -> skip
    ;

LineComment
    :   '//' ~[\r\n]* -> skip
    ;
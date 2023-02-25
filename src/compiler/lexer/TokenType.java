/**
 * @ Author: turk
 * @ Description: Vrste leksikalnih simbolov.
 */

package compiler.lexer;

public enum TokenType {
    /**
     * Konec datoteke.
     */
    EOF,

    /**
     * Ključne besede:
     */
    KW_ARR,
    KW_ELSE,
    KW_FOR,
    KW_FUN,
    KW_IF,
    KW_THEN,
    KW_TYP,
    KW_VAR,
    KW_WHERE,
    KW_WHILE,

    /**
     * Atomarni podatkovni tipi:
     */
    AT_LOGICAL, // atomarni tip `logical`
    AT_INTEGER, // atomarni tip `integer`
    AT_STRING,  // atomarni tip `string`

    /**
     * Konstante:
     */
    C_LOGICAL, // logična konstanta (true/false)
    C_INTEGER, // celoštevilska konstanta
    C_STRING,  // znakovna konstanta

    /**
     * Ime.
     */
    IDENTIFIER,

    /**
     * Operatorji:
     */
    OP_ADD,       // +
    OP_SUB,       // -
    OP_MUL,       // *
    OP_DIV,       // /
    OP_MOD,       // %

    OP_AND,       // &
    OP_OR,        // |
    OP_NOT,       // !
    
    OP_EQ,        // ==
    OP_NEQ,       // !=
    OP_LT,        // <
    OP_GT,        // >
    OP_LEQ,       // <=
    OP_GEQ,       // >=

    OP_LPARENT,   // (
    OP_RPARENT,   // )
    OP_LBRACKET,  // [
    OP_RBRACKET,  // ]
    OP_LBRACE,    // {
    OP_RBRACE,    // }

    OP_COLON,     // :
    OP_SEMICOLON, // ;
    OP_DOT,       // .
    OP_COMMA,     // ,
    OP_ASSIGN     // =
}

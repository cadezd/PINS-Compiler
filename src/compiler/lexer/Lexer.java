/**
 * @Author: turk
 * @Description: Leksikalni analizator.
 */

package compiler.lexer;

import common.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static common.RequireNonNull.requireNonNull;
import static compiler.lexer.TokenType.*;

public class Lexer {
    /**
     * Izvorna koda.
     */
    private final String source;

    /**
     * Preslikava iz ključnih besed v vrste simbolov.
     */
    private final static Map<String, TokenType> keywordMapping;

    static {
        keywordMapping = new HashMap<>();
        for (var token : TokenType.values()) {
            var str = token.toString();
            if (str.startsWith("KW_")) {
                keywordMapping.put(str.substring("KW_".length()).toLowerCase(), token);
            }
            if (str.startsWith("AT_")) {
                keywordMapping.put(str.substring("AT_".length()).toLowerCase(), token);
            }
        }
    }

    /**
     * Ustvari nov analizator.
     *
     * @param source Izvorna koda programa.
     */
    public Lexer(String source) {
        requireNonNull(source);
        this.source = source;
    }

    /**
     * Izvedi leksikalno analizo.
     *
     * @return seznam leksikalnih simbolov.
     */
    public List<Symbol> scan() {
        // IMPLEMENTACIJA LEKSIKALNE ANALIZE
        var symbols = new ArrayList<Symbol>();

        int state = 0;
        Symbol symbol;
        StringBuilder lexeme = new StringBuilder();
        CharStream charStream = new CharStream(this.source);

        A:
        while (true) {
            switch (state) {

                // START INITIAL STATE
                case 0 -> {
                    char c = charStream.nextChar();

                    lexeme.append(c);
                    if (c == ' ' | c == '\t' | c == '\r' | c == '\n') state = 1; // REMOVING WHITE TEXT
                    else if (c == '+') state = 2; // OPERATORS
                    else if (c == '-') state = 3;
                    else if (c == '*') state = 4;
                    else if (c == '/') state = 5;
                    else if (c == '%') state = 6;
                    else if (c == '&') state = 7;
                    else if (c == '|') state = 8;
                    else if (c == '(') state = 9;
                    else if (c == ')') state = 10;
                    else if (c == '[') state = 11;
                    else if (c == ']') state = 12;
                    else if (c == '{') state = 13;
                    else if (c == '}') state = 14;
                    else if (c == ':') state = 15;
                    else if (c == ';') state = 16;
                    else if (c == '.') state = 17;
                    else if (c == ',') state = 18;
                    else if (c == '!') state = 19;
                    else if (c == '=') state = 22;
                    else if (c == '>') state = 25;
                    else if (c == '<') state = 28;
                    else if (c >= '0' && c <= '9') state = 31; // INTEGER CONSTANTS
                    else if (c == 't') state = 34; // LOGICAL CONSTANTS
                    else if (c == 'f') state = 39;
                    else if (c == '\'') state = 45; // STRING CONSTANTS
                    else if (c == '#') state = 48; // COMMENTS
                    else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '_'))
                        state = 98; // KEYWORDS AND IDENTIFIERS
                    else if (c == '\0') state = 100; // EOF
                    else { // handle exception: invalid character
                        handleError(charStream, lexeme.toString(), "PINS: invalid character", 1);
                    }
                }

                // END INITIAL STATE

                // START WHITE TEXT

                case 1 -> {
                    lexeme.deleteCharAt(lexeme.length() - 1); // deleting white text
                    state = 0;
                }

                // END WHITE TEXT

                // START OPERATORS

                case 2 -> {                                                                                                  // TEMPLATE
                    symbol = getSymbol(lexeme.toString(), OP_ADD, charStream);                                               // creates new symbol
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);                                                  // adds symbol to list and clears lexeme
                    state = 0;                                                                                               // goes to initial state
                }

                case 3 -> {
                    symbol = getSymbol(lexeme.toString(), OP_SUB, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 4 -> {
                    symbol = getSymbol(lexeme.toString(), OP_MUL, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 5 -> {
                    symbol = getSymbol(lexeme.toString(), OP_DIV, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 6 -> {
                    symbol = getSymbol(lexeme.toString(), OP_MOD, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 7 -> {
                    symbol = getSymbol(lexeme.toString(), OP_AND, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 8 -> {
                    symbol = getSymbol(lexeme.toString(), OP_OR, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 9 -> {
                    symbol = getSymbol(lexeme.toString(), OP_LPARENT, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 10 -> {
                    symbol = getSymbol(lexeme.toString(), OP_RPARENT, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 11 -> {
                    symbol = getSymbol(lexeme.toString(), OP_LBRACKET, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 12 -> {
                    symbol = getSymbol(lexeme.toString(), OP_RBRACKET, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 13 -> {
                    symbol = getSymbol(lexeme.toString(), OP_LBRACE, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 14 -> {
                    symbol = getSymbol(lexeme.toString(), OP_RBRACE, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 15 -> {
                    symbol = getSymbol(lexeme.toString(), OP_COLON, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 16 -> {
                    symbol = getSymbol(lexeme.toString(), OP_SEMICOLON, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 17 -> {
                    symbol = getSymbol(lexeme.toString(), OP_DOT, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 18 -> {
                    symbol = getSymbol(lexeme.toString(), OP_COMMA, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 19 -> {
                    char c = charStream.nextChar();

                    if (c == '=') {
                        lexeme.append(c);
                        state = 20;
                    } else if (c == '\0') { // to avoid infinite loop
                        state = 21;
                    } else {
                        charStream.back(); // one char back
                        state = 21;
                    }
                }

                case 20 -> {
                    symbol = getSymbol(lexeme.toString(), OP_NEQ, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 21 -> {
                    symbol = getSymbol(lexeme.toString(), OP_NOT, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 22 -> {
                    char c = charStream.nextChar();

                    if (c == '=') {
                        lexeme.append(c);
                        state = 23;
                    } else if (c == '\0') { // to avoid infinite loop
                        state = 24;
                    } else {
                        charStream.back(); // one char back
                        state = 24;
                    }
                }

                case 23 -> {
                    symbol = getSymbol(lexeme.toString(), OP_EQ, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 24 -> {
                    symbol = getSymbol(lexeme.toString(), OP_ASSIGN, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 25 -> {
                    char c = charStream.nextChar();

                    if (c == '=') {
                        lexeme.append(c);
                        state = 26;
                    } else if (c == '\0') { // to avoid infinite loop
                        state = 27;
                    } else {
                        charStream.back(); // one char back
                        state = 27;
                    }
                }

                case 26 -> {
                    symbol = getSymbol(lexeme.toString(), OP_GEQ, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 27 -> {
                    symbol = getSymbol(lexeme.toString(), OP_GT, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 28 -> {
                    char c = charStream.nextChar();

                    if (c == '=') {
                        lexeme.append(c);
                        state = 29;
                    } else if (c == '\0') { // to avoid infinite loop
                        state = 30;
                    } else {
                        charStream.back(); // one char back
                        state = 30;
                    }
                }

                case 29 -> {
                    symbol = getSymbol(lexeme.toString(), OP_LEQ, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 30 -> {
                    symbol = getSymbol(lexeme.toString(), OP_LT, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                // END OPERATORS

                // START INTEGER CONSTANTS

                case 31 -> {
                    char c = charStream.nextChar();

                    if (c >= '0' && c <= '9') {
                        lexeme.append(c);
                        state = 31; // loops until there are numbers
                    } else if (c == '\0') {
                        state = 32;
                    } else {
                        charStream.back();
                        state = 32;
                    }
                }

                case 32 -> {
                    symbol = getSymbol(lexeme.toString(), C_INTEGER, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                // END INTEGER CONSTANTS

                // START LOGICAL CONSTANTS

                case 34 -> {
                    char c = charStream.nextChar();

                    if (c == 'r') {
                        lexeme.append(c);
                        state = 35;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 35 -> {
                    char c = charStream.nextChar();

                    if (c == 'u') {
                        lexeme.append(c);
                        state = 36;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 36 -> {
                    char c = charStream.nextChar();

                    if (c == 'e') {
                        lexeme.append(c);
                        state = 37;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 37 -> {
                    char c = charStream.nextChar();

                    if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else if (c == '\0') {
                        state = 38;
                    } else {
                        charStream.back();
                        state = 38;
                    }
                }

                case 38 -> {
                    symbol = getSymbol(lexeme.toString(), C_LOGICAL, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                case 39 -> {
                    char c = charStream.nextChar();

                    if (c == 'a') {
                        lexeme.append(c);
                        state = 40;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 40 -> {
                    char c = charStream.nextChar();

                    if (c == 'l') {
                        lexeme.append(c);
                        state = 41;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 41 -> {
                    char c = charStream.nextChar();

                    if (c == 's') {
                        lexeme.append(c);
                        state = 42;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 42 -> {
                    char c = charStream.nextChar();

                    if (c == 'e') {
                        lexeme.append(c);
                        state = 43;
                    } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 43 -> {
                    char c = charStream.nextChar();

                    if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else if (c == '\0') {
                        state = 44;
                    } else {
                        charStream.back();
                        state = 44;
                    }
                }

                case 44 -> {
                    symbol = getSymbol(lexeme.toString(), C_LOGICAL, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                // END LOGICAL CONSTANTS

                // START STRING CONSTANTS

                case 45 -> {
                    char c = charStream.nextChar();

                    if (c == '\'') {
                        lexeme.append(c);
                        state = 46;
                    } else if (c >= ' ' && c <= '~') {
                        lexeme.append(c);
                        state = 45;
                    } else {
                        //  handle exception: invalid character or unclosed string literal
                        if (c == '\t' | c == '\r' | c == '\n' || c == '\0')
                            handleError(charStream, lexeme.toString(), "PINS: unclosed string literal", 0);
                        else
                            handleError(charStream, lexeme.toString(), "PINS: invalid character", 1);
                    }
                }

                case 46 -> {
                    char c = charStream.nextChar();

                    if (c == '\'') {
                        lexeme.append(c);
                        state = 45;
                    } else if (c == '\0') {
                        state = 47;
                    } else {
                        charStream.back();
                        state = 47;
                    }
                }

                case 47 -> {
                    symbol = getSymbol(lexeme.toString(), C_STRING, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                // END STRING CONSTANTS

                // START COMMENTS

                case 48 -> {
                    char c = charStream.nextChar();

                    if (!(c == '\r' || c == '\n' || c == '\0')) {
                        lexeme.append(c);
                        state = 48;
                    } else {
                        state = 49;
                    }
                }

                case 49 -> {
                    lexeme.setLength(0); // deleting comment
                    state = 0;
                }

                // END COMMENTS

                // START KEYWORDS, IDENTIFIERS

                case 98 -> {
                    char c = charStream.nextChar();

                    if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
                        lexeme.append(c);
                        state = 98;
                    } else if (c == '\0') {
                        state = 99;
                    } else {
                        charStream.back();
                        state = 99;
                    }
                }

                case 99 -> {
                    symbol = getSymbol(lexeme.toString(), this.keywordMapping.getOrDefault(lexeme.toString(), IDENTIFIER), charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    state = 0;
                }

                // START KEYWORDS, IDENTIFIERS

                // START EOF

                case 100 -> {
                    lexeme = new StringBuilder("$");
                    symbol = getSymbol(lexeme.toString(), EOF, charStream);
                    addSymbolToListAndClearLexeme(symbol, symbols, lexeme);
                    break A;
                }

                // START EOF
            }
        }

        return symbols;
    }

    /*AUXILIARY METHODS*/
    private static void handleError(CharStream charStream, String lexeme, String message, int pos) {
        int endLine = charStream.getLine();
        int endColumn = charStream.getColumn();

        if (pos == 0) {
            Report.error(
                    new Position(endLine, endColumn - lexeme.length(), endLine, endColumn - lexeme.length()),
                    message);
        } else {
            Report.error(
                    new Position(endLine, endColumn - 1, endLine, endColumn - 1),
                    message);
        }
    }

    private static void addSymbolToListAndClearLexeme(Symbol symbol, ArrayList<Symbol> symbols, StringBuilder lexeme) {
        symbols.add(symbol);
        lexeme.setLength(0);
    }

    private static Symbol getSymbol(String lexeme, TokenType type, CharStream charStream) {
        int endLine = charStream.getLine();
        int endColumn = charStream.getColumn();

        if (type == C_STRING) {
            return new Symbol(new Position(endLine, endColumn - lexeme.length(), endLine, endColumn), type, lexeme.substring(1, lexeme.length() - 1).replace("''", "'"));
        }

        return new Symbol(new Position(endLine, endColumn - lexeme.length(), endLine, endColumn), type, lexeme);
    }
}
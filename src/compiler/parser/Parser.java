/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static compiler.common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import compiler.common.Report;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;

public class Parser {
    /**
     * Seznam leksikalnih simbolov.
     */
    private final List<Symbol> symbols;

    /**
     * Ciljni tok, kamor izpisujemo produkcije. Če produkcij ne želimo izpisovati,
     * vrednost opcijske spremenljivke nastavimo na Optional.empty().
     */
    private final Optional<PrintStream> productionsOutputStream;

    public Parser(List<Symbol> symbols, Optional<PrintStream> productionsOutputStream) {
        requireNonNull(symbols, productionsOutputStream);
        this.symbols = symbols;
        this.productionsOutputStream = productionsOutputStream;
    }

    /**
     * Izvedi sintaksno analizo.
     */
    public void parse() {
        parseSource();
    }

    private void parseSource() {
        dump("source -> definitions");
        parseDefinitions();
    }

    private void parseDefinitions() {
        dump("definitions -> definition definitions1");
        parseDefinition();
        parseDefinition1();

        if (!check(EOF) && !check(OP_RBRACE))
            Report.error(getSymbol().position, "PINS error: ';' or '}' expected");
    }

    private void parseDefinition() {
        if (check(KW_TYP)) {
            dump("definition -> type_definition");
            skip();
            parseTypeDefinition();
        } else if (check(KW_FUN)) {
            dump("definition -> function_definition");
            skip();
            parseFunctionDefinition();
        } else if (check(KW_VAR)) {
            dump("definition -> variable_definition ");
            skip();
            parseVariableDefinition();
        } else {
            Report.error(getSymbol().position, "PINS error: not a statement");
        }
    }

    private void parseDefinition1() {
        if (check(OP_SEMICOLON)) {
            dump("definitions1 -> \";\" definitions");
            skip();
            parseDefinitions();
        } else {
            dump("definitions1 -> epsylon");
        }
    }

    private void parseFunctionDefinition() {
        if (check(IDENTIFIER)) {
            dump("function_definition -> fun identifier \"(\" parameters \")\" \":\" type \"=\" expression");
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        }

        if (check(OP_LPARENT)) {
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: '(' expected");
        }

        parseParameters();

        if (check(OP_RPARENT)) {
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: ')' expected");
        }

        if (check(OP_COLON)) {
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: ':' expected");
        }

        parseType();

        if (check(OP_ASSIGN)) {
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: '=' expected");
        }

        parseExpression();
    }

    private void parseExpression() {
        dump("expression -> logical_ior_expression expression1");
        parseLogicalIORExpression();
        parseExpression1();
    }

    private void parseExpression1() {
        if (check(OP_LBRACE)) {
            dump("expression1 -> \"{\" WHERE definitions \"}\"");
            skip();

            if (check(KW_WHERE))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: WHERE keyword expected");
            }

            parseDefinitions();

            if (check(OP_RBRACE))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: '}' expected");
            }

        } else {
            dump("expression1 -> epsylon");
        }
    }

    private void parseLogicalIORExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression1");
        parseLogicalANDExpression();
        parseLogicalIORExpression1();
    }

    private void parseLogicalIORExpression1() {
        if (check(OP_OR)) {
            dump("logical_ior_expression1 -> \"|\" logical_ior_expression");
            skip();
            parseLogicalIORExpression();
        } else {
            dump("logical_ior_expression1 -> epsylon");
        }
    }


    private void parseLogicalANDExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression1");
        parseCompareExpression();
        parseLogicalANDExpression1();
    }

    private void parseLogicalANDExpression1() {
        if (check(OP_AND)) {
            dump("logical_and_expression1 -> \"&\" logical_and_expression");
            skip();
            parseLogicalANDExpression();
        } else {
            dump("logical_and_expression1 -> epsylon");
        }
    }

    private void parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression1");
        parseAdditiveExpression();
        parseCompareExpression1();
    }

    private void parseCompareExpression1() {
        if (check(OP_EQ)) {
            dump("compare_expression1 -> \"==\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression1 -> \"!=\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else if (check(OP_LEQ)) {
            dump("compare_expression1 -> \"<=\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else if (check(OP_GEQ)) {
            dump("compare_expression1 -> \">=\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else if (check(OP_LT)) {
            dump("compare_expression1 -> \"<\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else if (check(OP_GT)) {
            dump("compare_expression1 -> \">\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else {
            dump("compare_expression1 -> epsylon");
        }
    }

    private void parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression1");
        parseMultiplicativeExpression();
        parseAdditiveExpression1();
    }

    private void parseAdditiveExpression1() {
        if (check(OP_ADD)) {
            dump("additive_expression1 -> \"+\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else if (check(OP_SUB)) {
            dump("additive_expression1 -> \"-\" additive_expression");
            skip();
            parseAdditiveExpression();
        } else {
            dump("additive_expression1 -> epsylon");
        }
    }

    private void parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression1");
        parsePrefixExpression();
        parseMultiplicativeExpression1();
    }

    private void parseMultiplicativeExpression1() {
        if (check(OP_MUL)) {
            dump("multiplicative_expression1 ->  \"*\" multiplicative_expression");
            skip();
            parseMultiplicativeExpression();
        } else if (check(OP_DIV)) {
            dump("multiplicative_expression1 ->  \"/\" multiplicative_expression");
            skip();
            parseMultiplicativeExpression();
        } else if (check(OP_MOD)) {
            dump("multiplicative_expression1 ->  \"%\" multiplicative_expression");
            skip();
            parseMultiplicativeExpression();
        } else {
            dump("multiplicative_expression1 -> epsylon");
        }
    }

    private void parsePrefixExpression() {
        if (check(OP_ADD)) {
            dump("prefix_expression -> \"+\" prefix_expression");
            skip();
            parsePrefixExpression();
        } else if (check(OP_SUB)) {
            dump("prefix_expression -> \"-\" prefix_expression");
            skip();
            parsePrefixExpression();
        } else if (check(OP_NOT)) {
            dump("prefix_expression -> \"!\" prefix_expression");
            skip();
            parsePrefixExpression();
        } else {
            dump("prefix_expression -> postfix_expression");
            parsePostfixExpression();
        }
    }

    private void parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression1");
        parseAtomExpression();
        parsePostfixExpression1();
    }

    private void parsePostfixExpression1() {
        if (check(OP_LBRACKET)) {
            dump("postfix_expression1 -> \"[\" expression \"]\" postfix_expression1");
            skip();

            parseExpression();

            if (check(OP_RBRACKET))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: ']' expected");
            }

            parsePostfixExpression1();
        } else {
            dump("postfix_expression1 -> epsylon");
        }
    }

    private void parseAtomExpression() {
        if (check(C_LOGICAL)) {
            dump("atom_expression -> log_constant");
            skip();
        } else if (check(C_INTEGER)) {
            dump("atom_expression -> int_constant");
            skip();
        } else if (check(C_STRING)) {
            dump("atom_expression -> str_constant");
            skip();
        } else if (check(IDENTIFIER)) {
            dump("atom_expression -> identifier identifier1");
            skip();
            parseIdentifier1();
        } else if (check(OP_LPARENT)) {
            dump("atom_expression -> \"(\" expressions \")\"");
            skip();

            parseExpressions();

            if (check(OP_RPARENT))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: ')' expected");
            }
        } else if (check(OP_LBRACE)) {
            dump("atom_expression -> \"{\" other_atom_expressions \"}\"");
            skip();

            parseOtnerAtomExpressions();

            if (check(OP_RBRACE))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: '}' expected");
            }
        } else {

            Report.error(getSymbol().position, "PINS error: not a statement");
        }
    }

    private void parseIdentifier1() {
        if (check(OP_LPARENT)) {
            dump("identifier1 -> \"(\" expressions \")\"");
            skip();

            parseExpressions();

            if (check(OP_RPARENT))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: ')' expected");
            }
        } else {
            dump("identifier1 -> epsylon");
        }
    }

    private void parseOtnerAtomExpressions() {
        if (check(KW_IF)) {
            dump("other_atom_expressions -> if_else_expression if_then_else_expression");
            skip();
            parseIfElseExpression();
            parseIfThenElseExpression();
        } else if (check(KW_WHILE)) {
            dump("other_atom_expressions ->  while_expression ");
            skip();
            parseWhileExpression();
        } else if (check(KW_FOR)) {
            dump("other_atom_expressions -> for_expression");
            skip();
            parseForExpression();
        } else {
            dump("other_atom_expressions -> expression \"=\" expression");
            parseExpression();

            if (check(OP_ASSIGN))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: '=' expected");
            }

            parseExpression();
        }
    }

    private void parseIfElseExpression() {
        dump("if_else_expression -> if expression then expression");
        parseExpression();

        if (check(KW_THEN))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: THEN keyword expected");
        }

        parseExpression();
    }

    private void parseIfThenElseExpression() {
        if (check(KW_ELSE)) {
            dump("if_then_else_expression -> else expression");
            skip();
            parseExpression();
        } else {
            dump("if_then_else_expression -> epsylon");
        }
    }

    private void parseWhileExpression() {
        dump("while_expression -> while expression \":\" expression");

        parseExpression();

        if (check(OP_COLON))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ':' expected");
        }

        parseExpression();
    }

    private void parseForExpression() {
        dump("for_expression ->  for identifier \"=\" expression \",\" expression \",\" expression \":\" expression");

        if (check(IDENTIFIER))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        }

        if (check(OP_ASSIGN))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: '=' expected " + getSymbol().lexeme);
        }

        parseExpression();

        if (check(OP_COMMA))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ',' expected");
        }

        parseExpression();

        if (check(OP_COMMA))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ',' expected");
        }

        parseExpression();

        if (check(OP_COLON))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ':' expected");
        }

        parseExpression();
    }


    private void parseExpressions() {
        dump("expressions -> expression expressions1 ");
        parseExpression();
        parseExpressions1();
    }

    private void parseExpressions1() {
        if (check(OP_COMMA)) {
            dump("expressions1 -> \",\" expressions");
            skip();
            parseExpressions();
        } else {
            dump("expressions1 -> epsylon");
        }
    }


    private void parseParameters() {
        dump("parameters -> parameter parameters1");
        parseParameter();
        parseParameters1();
    }

    private void parseParameters1() {
        if (check(OP_COMMA)) {
            dump("parameters1 ->  \",\" parameters");
            skip();
            parseParameters();
        } else {
            dump("parameters1 -> epsylon");
        }
    }

    private void parseParameter() {
        if (check(IDENTIFIER)) {
            dump("parameter -> identifier \":\" type ");
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        }

        if (check(OP_COLON))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ':' expected");
        }

        parseType();
    }

    private void parseVariableDefinition() {
        if (check(IDENTIFIER)) {
            dump("variable_definition -> var identifier \":\" type");
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        }

        if (check(OP_COLON))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ':' expected");
        }

        parseType();
    }

    private void parseTypeDefinition() {
        if (check(IDENTIFIER)) {
            dump("type_definition -> typ identifier \":\" type");
            skip();
        } else {
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        }

        if (check(OP_COLON))
            skip();
        else {
            Report.error(getSymbol().position, "PINS error: ':' expected");
        }

        parseType();
    }

    private void parseType() {
        if (check(IDENTIFIER)) {
            dump("type -> identifier");
            skip();
        } else if (check(AT_LOGICAL)) {
            dump("type -> logical");
            skip();
        } else if (check(AT_INTEGER)) {
            dump("type -> integer");
            skip();
        } else if (check(AT_STRING)) {
            dump("type -> string");
            skip();
        } else if (check(KW_ARR)) {
            dump("type -> arr \"[\" int_const \"]\" type ");
            skip();

            if (check(OP_LBRACKET))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: '[' expected");
            }

            if (check(C_INTEGER))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: array dimension missing");
            }

            if (check(OP_RBRACKET))
                skip();
            else {
                Report.error(getSymbol().position, "PINS error: ']' expected");
            }

            parseType();
        } else {
            Report.error(getSymbol().position, "PINS error: type definition required");
        }
    }


    /*AUXILIARY METHODS*/

    private boolean check(TokenType type) {
        return this.symbols.get(0).tokenType.equals(type);
    }

    private void skip() {
        this.symbols.remove(0);
    }

    private Symbol getSymbol() {
        return this.symbols.get(0);
    }

    /**
     * Izpiše produkcijo na izhodni tok.
     */
    private void dump(String production) {
        if (productionsOutputStream.isPresent()) {
            productionsOutputStream.get().println(production);
        }
    }
}

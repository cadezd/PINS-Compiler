/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static compiler.common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import compiler.common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;
import compiler.parser.ast.Ast;
import compiler.parser.ast.def.*;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.Type;
import compiler.parser.ast.type.TypeName;

import javax.swing.plaf.DesktopPaneUI;

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
    public Ast parse() {
        return parseSource();
    }

    private Ast parseSource() {
        dump("source -> definitions");
        return parseDefinitions();
    }

    private Ast parseDefinitions() {
        // TODO: dodelaj
        dump("definitions -> definition definitions1");
        List<Def> definitions = new ArrayList<>();
        definitions.add(parseDefinition());
        parseDefinition1(definitions);

        if (!check(EOF) && !check(OP_RBRACE))
            Report.error(getSymbol().position, "PINS error: ';' or '}' expected");

        Position.Location start = definitions.get(0).position.start;
        Position.Location end = definitions.get(definitions.size() - 1).position.end;
        return new Defs(
                new Position(start, end),
                definitions
        );
    }

    private Def parseDefinition() {
        if (check(KW_TYP)) {
            dump("definition -> type_definition");
            Symbol typSymbol = skip();
            return parseTypeDefinition(typSymbol);

        } else if (check(KW_FUN)) {
            // TODO: dodelaj
            dump("definition -> function_definition");
            Symbol funSymbol = skip();
            return parseFunctionDefinition(funSymbol);

        } else if (check(KW_VAR)) {
            dump("definition -> variable_definition ");
            Symbol varSymbol = skip();
            return parseVariableDefinition(varSymbol);

        } else {
            Report.error(getSymbol().position, "PINS error: not a statement");
            return null;
        }
    }

    private void parseDefinition1(List<Def> definitions) {
        if (check(OP_SEMICOLON)) {
            dump("definitions1 -> \";\" definitions");
            skip();
            definitions.add(parseDefinition());
            parseDefinition1(definitions);
        } else {
            dump("definitions1 -> epsylon");
        }
    }

    private Def parseFunctionDefinition(Symbol startSymbol) {
        if (!check(IDENTIFIER))
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        dump("function_definition -> fun identifier \"(\" parameters \")\" \":\" type \"=\" expression");
        Symbol funIdentifier = skip();

        if (!check(OP_LPARENT))
            Report.error(getSymbol().position, "PINS error: '(' expected");
        skip();

        List<FunDef.Parameter> funParameters = parseParameters();

        if (!check(OP_RPARENT))
            Report.error(getSymbol().position, "PINS error: ')' expected");
        skip();

        if (!check(OP_COLON))
            Report.error(getSymbol().position, "PINS error: ':' expected");
        skip();

        Type funType = parseType();

        if (!check(OP_ASSIGN))
            Report.error(getSymbol().position, "PINS error: '=' expected");
        skip();

        // TODO: dodelaj expressions
        Expr funExpression = parseExpression();
        return new FunDef(
                new Position(startSymbol.position.start, funExpression.position.end),
                funIdentifier.lexeme,
                funParameters,
                funType,
                funExpression
        );
    }

    private Expr parseExpression() {
        //  TODO: dodelaj
        dump("expression -> logical_ior_expression expression1");
        parseLogicalIORExpression();
        parseExpression1();
        return null;
    }

    private void parseExpression1() {
        //  TODO: dodelaj
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
        //  TODO: dodelaj
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression1");
        parseLogicalANDExpression();
        parseLogicalIORExpression1();
    }

    private void parseLogicalIORExpression1() {
        //  TODO: dodelaj
        if (check(OP_OR)) {
            dump("logical_ior_expression1 -> \"|\" logical_ior_expression");
            skip();
            parseLogicalANDExpression();
            parseLogicalIORExpression1();
        } else {
            dump("logical_ior_expression1 -> epsylon");
        }
    }


    private void parseLogicalANDExpression() {
        //  TODO: dodelaj
        dump("logical_and_expression -> compare_expression logical_and_expression1");
        parseCompareExpression();
        parseLogicalANDExpression1();
    }

    private void parseLogicalANDExpression1() {
        //  TODO: dodelaj
        if (check(OP_AND)) {
            dump("logical_and_expression1 -> \"&\" logical_and_expression");
            skip();
            parseCompareExpression();
            parseLogicalANDExpression1();
        } else {
            dump("logical_and_expression1 -> epsylon");
        }
    }

    private void parseCompareExpression() {
        //  TODO: dodelaj
        dump("compare_expression -> additive_expression compare_expression1");
        parseAdditiveExpression();
        parseCompareExpression1();
    }

    private void parseCompareExpression1() {
        //  TODO: dodelaj
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
        //  TODO: dodelaj
        dump("additive_expression -> multiplicative_expression additive_expression1");
        parseMultiplicativeExpression();
        parseAdditiveExpression1();
    }

    private void parseAdditiveExpression1() {
        //  TODO: dodelaj
        if (check(OP_ADD)) {
            dump("additive_expression1 -> \"+\" additive_expression");
            skip();
            parseMultiplicativeExpression();
            parseAdditiveExpression1();
        } else if (check(OP_SUB)) {
            dump("additive_expression1 -> \"-\" additive_expression");
            skip();
            parseMultiplicativeExpression();
            parseAdditiveExpression1();
        } else {
            dump("additive_expression1 -> epsylon");
        }
    }

    private void parseMultiplicativeExpression() {
        //  TODO: dodelaj
        dump("multiplicative_expression -> prefix_expression multiplicative_expression1");
        parsePrefixExpression();
        parseMultiplicativeExpression1();
    }

    private void parseMultiplicativeExpression1() {
        //  TODO: dodelaj
        if (check(OP_MUL)) {
            dump("multiplicative_expression1 ->  \"*\" multiplicative_expression");
            skip();
            parsePrefixExpression();
            parseMultiplicativeExpression1();
        } else if (check(OP_DIV)) {
            dump("multiplicative_expression1 ->  \"/\" multiplicative_expression");
            skip();
            parsePrefixExpression();
            parseMultiplicativeExpression1();
        } else if (check(OP_MOD)) {
            dump("multiplicative_expression1 ->  \"%\" multiplicative_expression");
            skip();
            parsePrefixExpression();
            parseMultiplicativeExpression1();
        } else {
            dump("multiplicative_expression1 -> epsylon");
        }
    }

    private void parsePrefixExpression() {
        //  TODO: dodelaj
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
        //  TODO: dodelaj
        dump("postfix_expression -> atom_expression postfix_expression1");
        parseAtomExpression();
        parsePostfixExpression1();
    }

    private void parsePostfixExpression1() {
        //  TODO: dodelaj
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

    private Expr parseAtomExpression() {
        //  TODO: dodelaj
        if (check(C_LOGICAL)) {
            dump("atom_expression -> log_constant");
            Symbol atmExprLogical = skip();
            return new Literal(
                    atmExprLogical.position,
                    atmExprLogical.lexeme,
                    Atom.Type.LOG
            );

        } else if (check(C_INTEGER)) {
            dump("atom_expression -> int_constant");
            Symbol atmExprInteger = skip();
            return new Literal(
                    atmExprInteger.position,
                    atmExprInteger.lexeme,
                    Atom.Type.INT
            );

        } else if (check(C_STRING)) {
            dump("atom_expression -> str_constant");
            Symbol atmExprString = skip();
            return new Literal(
                    atmExprString.position,
                    atmExprString.lexeme,
                    Atom.Type.STR
            );

        } else if (check(IDENTIFIER)) {
            // TODO: dodelaj
            dump("atom_expression -> identifier identifier1");
            Symbol identifier = skip();
            return parseIdentifier1(identifier);

        } else if (check(OP_LPARENT)) {
            dump("atom_expression -> \"(\" expressions \")\"");
            skip();

            Block exppressions = parseExpressions();

            if (!check(OP_RPARENT))
                Report.error(getSymbol().position, "PINS error: ')' expected");
            skip();

            return exppressions;

        } else if (check(OP_LBRACE)) {
            dump("atom_expression -> \"{\" other_atom_expressions \"}\"");
            skip();

            Expr otherAtomExpressions = parseOtherAtomExpressions();

            if (!check(OP_RBRACE))
                Report.error(getSymbol().position, "PINS error: '}' expected");
            skip();

            return otherAtomExpressions;
        } else {
            Report.error(getSymbol().position, "PINS error: not a statement");
            return null;
        }
    }

    private Expr parseIdentifier1(Symbol identifier) {
        //  TODO: dodelaj
        if (check(OP_LPARENT)) {
            dump("identifier1 -> \"(\" expressions \")\"");
            skip();

            Block expressions = parseExpressions();

            if (!check(OP_RPARENT))
                Report.error(getSymbol().position, "PINS error: ')' expected");
            Symbol endSymbol = skip();

            return new Call(
                    new Position(identifier.position.start, endSymbol.position.end),
                    expressions.expressions,
                    identifier.lexeme
            );
        } else {
            dump("identifier1 -> epsylon");
            return new Name(
                    identifier.position,
                    identifier.lexeme
            );
        }
    }

    private Expr parseOtherAtomExpressions() {
        if (check(KW_IF)) {
            dump("other_atom_expressions -> if_else_expression if_then_else_expression");
            Symbol ifSymbol = skip();
            IfThenElse ifThen = parseIfElseExpression(ifSymbol);
            return parseIfThenElseExpression(ifThen);

        } else if (check(KW_WHILE)) {
            dump("other_atom_expressions ->  while_expression ");
            Symbol whileSymbol = skip();
            return parseWhileExpression(whileSymbol);

        } else if (check(KW_FOR)) {
            dump("other_atom_expressions -> for_expression");
            Symbol forSymbol = skip();
            return parseForExpression(forSymbol);

        } else {
            dump("other_atom_expressions -> expression \"=\" expression");
            Expr leftExpression = parseExpression();

            if (!check(OP_ASSIGN))
                Report.error(getSymbol().position, "PINS error: '=' expected");
            skip();

            Expr rightExpression = parseExpression();

            return new Binary(
                    new Position(leftExpression.position.start, rightExpression.position.end),
                    leftExpression,
                    Binary.Operator.ASSIGN,
                    rightExpression
            );
        }
    }

    private IfThenElse parseIfElseExpression(Symbol startSymbol) {
        dump("if_else_expression -> if expression then expression");
        Expr condition = parseExpression();

        if (!check(KW_THEN))
            Report.error(getSymbol().position, "PINS error: THEN keyword expected");
        skip();

        Expr thenExpression = parseExpression();

        return new IfThenElse(
                new Position(startSymbol.position.start, thenExpression.position.end),
                condition, thenExpression
        );
    }

    private Expr parseIfThenElseExpression(IfThenElse ifThen) {
        if (check(KW_ELSE)) {
            dump("if_then_else_expression -> else expression");
            skip();
            Expr elseExpression = parseExpression();
            return new IfThenElse(
                    new Position(ifThen.position.start, elseExpression.position.end),
                    ifThen.condition,
                    ifThen.thenExpression,
                    ifThen.elseExpression
            );
        } else {
            dump("if_then_else_expression -> epsylon");
            return ifThen;
        }
    }

    private Expr parseWhileExpression(Symbol startSymbol) {
        dump("while_expression -> while expression \":\" expression");

        Expr condition = parseExpression();

        if (!check(OP_COLON))
            Report.error(getSymbol().position, "PINS error: ':' expected");
        skip();

        Expr body = parseExpression();

        return new While(
                new Position(startSymbol.position.start, body.position.end),
                condition,
                body
        );
    }

    private Expr parseForExpression(Symbol startSymbol) {
        dump("for_expression ->  for identifier \"=\" expression \",\" expression \",\" expression \":\" expression");

        if (!check(IDENTIFIER))
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        Symbol forIdentifier = skip();

        if (!check(OP_ASSIGN))
            Report.error(getSymbol().position, "PINS error: '=' expected " + getSymbol().lexeme);
        skip();

        Expr low = parseExpression();

        if (!check(OP_COMMA))
            Report.error(getSymbol().position, "PINS error: ',' expected");
        skip();

        Expr high = parseExpression();

        if (!check(OP_COMMA))
            Report.error(getSymbol().position, "PINS error: ',' expected");
        skip();

        Expr step = parseExpression();

        if (!check(OP_COLON))
            Report.error(getSymbol().position, "PINS error: ':' expected");
        skip();

        Expr body = parseExpression();

        return new For(
                new Position(startSymbol.position.start, body.position.end),
                new Name(forIdentifier.position, forIdentifier.lexeme),
                low,
                high,
                step,
                body
        );
    }


    private Block parseExpressions() {
        dump("expressions -> expression expressions1 ");
        List<Expr> expressions = new ArrayList<>();
        expressions.add(parseExpression());
        parseExpressions1(expressions);

        Position.Location start = expressions.get(0).position.start;
        Position.Location end = expressions.get(expressions.size() - 1).position.end;
        return new Block(
                new Position(start, end),
                expressions
        );
    }

    private void parseExpressions1(List<Expr> expressions) {
        if (check(OP_COMMA)) {
            dump("expressions1 -> \",\" expressions");
            skip();
            expressions.add(parseExpression());
            parseExpressions1(expressions);
        } else {
            dump("expressions1 -> epsylon");
        }
    }


    private List<FunDef.Parameter> parseParameters() {
        dump("parameters -> parameter parameters1");
        List<FunDef.Parameter> parameters = new ArrayList<>();
        parameters.add(parseParameter());
        parseParameters1(parameters);
        return parameters;
    }

    private void parseParameters1(List<FunDef.Parameter> parameters) {
        if (check(OP_COMMA)) {
            dump("parameters1 ->  \",\" parameters");
            skip();
            parameters.add(parseParameter());
            parseParameters1(parameters);
        } else {
            dump("parameters1 -> epsylon");
        }
    }

    private FunDef.Parameter parseParameter() {
        if (!check(IDENTIFIER))
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        dump("parameter -> identifier \":\" type ");
        Symbol parIdentifier = skip();

        if (!check(OP_COLON))
            Report.error(getSymbol().position, "PINS error: ':' expected");
        skip();

        Type parType = parseType();

        return new FunDef.Parameter(
                new Position(parType.position.start, parType.position.end),
                parIdentifier.lexeme,
                parType
        );
    }

    private Def parseVariableDefinition(Symbol startSymbol) {
        if (!check(IDENTIFIER))
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        dump("variable_definition -> var identifier \":\" type");
        Symbol varIdentifier = skip();

        if (!check(OP_COLON))
            Report.error(getSymbol().position, "PINS error: ':' expected");
        skip();

        Type varType = parseType();

        return new VarDef(
                new Position(startSymbol.position.start, varType.position.end),
                varIdentifier.lexeme,
                varType
        );
    }

    private Def parseTypeDefinition(Symbol startSymbol) {
        if (!check(IDENTIFIER))
            Report.error(getSymbol().position, "PINS error: <identifier> expected");
        dump("type_definition -> typ identifier \":\" type");
        Symbol typIdentifier = skip();

        if (!check(OP_COLON))
            Report.error(getSymbol().position, "PINS error: ':' expected");
        skip();

        Type typType = parseType();

        return new TypeDef(
                new Position(startSymbol.position.start, typType.position.end),
                typIdentifier.lexeme,
                typType
        );
    }

    private Type parseType() {
        if (check(IDENTIFIER)) {
            dump("type -> identifier");
            Symbol symbol = skip();
            return new TypeName(symbol.position, symbol.lexeme);

        } else if (check(AT_LOGICAL)) {
            dump("type -> logical");
            Symbol symbol = skip();
            return Atom.LOG(symbol.position);

        } else if (check(AT_INTEGER)) {
            dump("type -> integer");
            Symbol symbol = skip();
            return Atom.INT(symbol.position);

        } else if (check(AT_STRING)) {
            dump("type -> string");
            Symbol symbol = skip();
            return Atom.STR(symbol.position);

        } else if (check(KW_ARR)) {
            dump("type -> arr \"[\" int_const \"]\" type ");
            Symbol startSymbol = skip();

            if (!check(OP_LBRACKET))
                Report.error(getSymbol().position, "PINS error: '[' expected");
            skip();

            if (!check(C_INTEGER))
                Report.error(getSymbol().position, "PINS error: array dimension missing");
            Symbol arraySize = skip();

            if (!check(OP_RBRACKET))
                Report.error(getSymbol().position, "PINS error: ']' expected");
            skip();

            Type type = parseType();

            return new Array(
                    new Position(startSymbol.position.start, type.position.end),
                    Integer.parseInt(arraySize.lexeme),
                    type
            );
        } else {
            Report.error(getSymbol().position, "PINS error: type definition required");
            return null;
        }
    }


    /*AUXILIARY METHODS*/

    private boolean check(TokenType type) {
        return this.symbols.get(0).tokenType.equals(type);
    }

    private Symbol skip() {
        return this.symbols.remove(0);
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

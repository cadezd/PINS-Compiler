/**
 * @ Author: turk
 * @ Description: Preverjanje in razreševanje imen.
 */

package compiler.seman.name;

import static common.RequireNonNull.requireNonNull;

import common.Report;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;
import compiler.seman.common.NodeDescription;
import compiler.seman.name.env.SymbolTable;
import compiler.seman.name.env.SymbolTable.DefinitionAlreadyExistsException;

import java.util.Optional;

public class NameChecker implements Visitor {
    /**
     * Opis vozlišč, ki jih povežemo z njihovimi
     * definicijami.
     */
    private NodeDescription<Def> definitions;

    /**
     * Simbolna tabela.
     */
    private SymbolTable symbolTable;

    /**
     * Ustvari nov razreševalnik imen.
     */
    public NameChecker(
            NodeDescription<Def> definitions,
            SymbolTable symbolTable
    ) {
        requireNonNull(definitions, symbolTable);
        this.definitions = definitions;
        this.symbolTable = symbolTable;
    }

    @Override
    public void visit(Call call) {
        // povezemo klic funkcije z definicijo
        Optional<Def> link = symbolTable.definitionFor(call.name);

        if (!link.isPresent())
            Report.error(call.position, "PINS error: function " + call.name + " is not defined");

        if (!(link.get() instanceof FunDef))
            Report.error(call.position, "PINS error: " + call.name + " is not a function");

        definitions.store(link.get(), call);

        // obdelamo argumente funkcije
        for (Expr arg : call.arguments)
            visitExpr(arg);
    }

    @Override
    public void visit(Binary binary) {
        if (binary.operator.equals(Binary.Operator.ARR) && binary.left instanceof Name name) {
            Optional<Def> link = symbolTable.definitionFor(name.name);

            if (!link.isPresent())
                Report.error(name.position, "PINS error: array " + name.name + " is not defined");

            if (!(link.get() instanceof VarDef varDef && varDef.type instanceof Array || link.get() instanceof Parameter parDef && parDef.type instanceof Array))
                Report.error(name.position, "PINS error: " + name.name + " is not an array");

            definitions.store(link.get(), name);
        }

        // obdelamo levi del
        visitExpr(binary.left);

        // obdelamo desni del
        visitExpr(binary.right);
    }

    @Override
    public void visit(Block block) {
        // obdelamo vse expression-e v bloku
        for (Expr expression : block.expressions)
            visitExpr(expression);
    }

    @Override
    public void visit(For forLoop) {
        // obdelamo for loop
        visit(forLoop.counter);
        visitExpr(forLoop.low);
        visitExpr(forLoop.high);
        visitExpr(forLoop.step);
        visitExpr(forLoop.body);
    }

    @Override
    public void visit(Name name) {
        // povezemo ime spremenljivke z njeno definicijo
        Optional<Def> link = symbolTable.definitionFor(name.name);

        if (!link.isPresent())
            Report.error(name.position, "PINS error: variable " + name.name + " is not defined");

        definitions.store(link.get(), name);
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        // obdelamo pogoj in then stavek
        visitExpr(ifThenElse.condition);
        visitExpr(ifThenElse.thenExpression);

        // obdelamo else stavek (ce je prisoten)
        if (ifThenElse.elseExpression.isPresent())
            visitExpr(ifThenElse.elseExpression.get());
    }

    @Override
    public void visit(Literal literal) {
        return;
    }

    @Override
    public void visit(Unary unary) {
        // obdelamo izraz
        visitExpr(unary.expr);
    }

    @Override
    public void visit(While whileLoop) {
        // obdelamo pogoj in telo while stavka
        visitExpr(whileLoop.condition);
        visitExpr(whileLoop.body);
    }

    @Override
    public void visit(Where where) {
        symbolTable.pushScope(); // nov notranji scope
        visit(where.defs); // obdelamo definicije
        visitExpr(where.expr); // obdelamo izraze
        symbolTable.popScope(); // nazaj v prejsnji scope
    }

    @Override
    public void visit(Defs defs) {
        // BFS (dodamo vse definicije v tabelo)
        for (Def definition : defs.definitions) {
            if (definition instanceof VarDef varDef) {  // dodaj definicijo spremenljivke
                addToSymbolTable(varDef, "PINS error: variable " + varDef.name + " is already defined");

            } else if (definition instanceof TypeDef typeDef) { // dodaj definicijo tipa
                addToSymbolTable(typeDef, "PINS error: type " + typeDef.name + " is already defined");

            } else if (definition instanceof FunDef funDef) { // dodaj definicijo funkcije
                addToSymbolTable(funDef, "PINS error: function " + funDef.name + " is already defined");

            } else {
                Report.error(definition.position, "PINS error: unknown definition");

            }
        }

        // DFS : gremo en nivo nizje (v globino)
        for (Def definition : defs.definitions) {
            if (definition instanceof VarDef varDef) {  // poveze spremenljivko z njeno definicijo
                visit(varDef);

            } else if (definition instanceof TypeDef typeDef) { // poveze tip z njegovo definicijo
                visit(typeDef);

            } else if (definition instanceof FunDef funDef) { // poveze funkcijo z njeno definicijo

                for (Parameter parameter : funDef.parameters) { // povežemo tipe parametrov z definicijami
                    visit(parameter);
                }

                if (funDef.type instanceof TypeName funType) { // povežemo return tip z definicijo
                    Optional<Def> link = symbolTable.definitionFor(funType.identifier);

                    if (!link.isPresent())
                        Report.error(funType.position, "PINS error: type " + funType.identifier + " is not defined");

                    if (!(link.get() instanceof TypeDef))
                        Report.error(funType.position, "PINS error: " + funType.identifier + " is not a type");

                    definitions.store(link.get(), funDef.type);
                }

                // dodamo definicije parametrov in obdelamo telo funkcije
                symbolTable.pushScope();
                visit(funDef);
                symbolTable.popScope();
            }
        }
    }

    @Override
    public void visit(FunDef funDef) {
        // dodamo vse definicije parametrov v simbolno tabelo
        for (Parameter parameter : funDef.parameters) { // dodaj definicijo parametra
            addToSymbolTable(parameter, "PINS error: parameter " + parameter.name + " is already defined");
        }

        // obdelamo telo funkcije
        visitExpr(funDef.body);
    }

    @Override
    public void visit(TypeDef typeDef) {
        if (typeDef.type instanceof TypeName typeType) { // ce je type nekega custom typa ga povezi z definicijo
            Optional<Def> link = symbolTable.definitionFor(typeType.identifier);

            if (!link.isPresent())
                Report.error(typeType.position, "PINS error: type " + typeType.identifier + " is not defined");

            if (!(link.get() instanceof TypeDef))
                Report.error(typeType.position, "PINS error: " + typeType.identifier + " is not a type");

            definitions.store(link.get(), typeType);
        }
    }

    @Override
    public void visit(VarDef varDef) {
        if (varDef.type instanceof TypeName varType) { // ce je spremenljivka nekega custom typa ga povezi z definicijo
            Optional<Def> link = symbolTable.definitionFor(varType.identifier);

            if (!link.isPresent())
                Report.error(varType.position, "PINS error: type " + varType.identifier + " is not defined");

            if (!(link.get() instanceof TypeDef))
                Report.error(varType.position, "PINS error: " + varType.identifier + " is not a type");

            definitions.store(link.get(), varType);
        }
    }

    @Override
    public void visit(Parameter parameter) {
        if (parameter.type instanceof TypeName parType) { // ce je parameter nekega custom typa ga povezi z definicijo
            Optional<Def> link = symbolTable.definitionFor(parType.identifier);

            if (!link.isPresent())
                Report.error(parType.position, "PINS error: type " + parType.identifier + " is not defined");

            if (!(link.get() instanceof TypeDef))
                Report.error(parType.position, "PINS error: " + parType.identifier + " is not a type");

            definitions.store(link.get(), parType);
        }
    }

    @Override
    public void visit(Array array) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Atom atom) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(TypeName name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    /*AUXILIARY METHODS*/

    public void visitExpr(Expr expression) {
        // navigacija med razlicnimi vozlisci
        if (expression instanceof Call exprCall) {
            visit(exprCall);
        } else if (expression instanceof Binary exprBinary) {
            visit(exprBinary);
        } else if (expression instanceof Block exprBlock) {
            visit(exprBlock);
        } else if (expression instanceof For exprFor) {
            visit(exprFor);
        } else if (expression instanceof Name exprName) {
            visit(exprName);
        } else if (expression instanceof IfThenElse exprIfThenElse) {
            visit(exprIfThenElse);
        } else if (expression instanceof Literal exprLiteral) {
            visit(exprLiteral);
        } else if (expression instanceof Unary exprUnary) {
            visit(exprUnary);
        } else if (expression instanceof While exprWhile) {
            visit(exprWhile);
        } else if (expression instanceof Where exprWhere) {
            visit(exprWhere);
        } else {
            Report.error(expression.position, "PINS error: something went wrong when executing name checker phase");
        }
    }

    private boolean addToSymbolTable(Def definition, String errorMessage) {
        try {
            symbolTable.insert(definition);
            return true;
        } catch (DefinitionAlreadyExistsException e) {
            Report.error(definition.position, errorMessage);
            return false;
        }
    }
}

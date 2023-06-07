/**
 * @ Author: turk
 * @ Description: Preverjanje in razreševanje imen.
 */

package compiler.seman.name;

import static common.RequireNonNull.requireNonNull;

import common.Constants;
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
        // Checking definitions for arguments
        for (Expr argument : call.arguments)
            argument.accept(this);

        // Handling standard library
        if (Constants.stdLibrary.containsKey(call.name))
            return;

        Optional<Def> funDef = symbolTable.definitionFor(call.name);
        if (funDef.isEmpty())
            Report.error(call.position, "PINS error: function " + call.name + " is not defined");
        else if (!(funDef.get() instanceof FunDef))
            Report.error(call.position, "PINS error: " + call.name + " is not a function");
        else
            definitions.store(funDef.get(), call);
    }

    @Override
    public void visit(Binary binary) {
        // Checking if array is defined
        if (binary.operator.equals(Binary.Operator.ARR) && binary.left instanceof Name name) {
            Optional<Def> link = symbolTable.definitionFor(name.name);

            if (link.isEmpty())
                Report.error(name.position, "PINS error: array " + name.name + " is not defined");
            else if (!(link.get() instanceof VarDef || link.get() instanceof Parameter))
                Report.error(name.position, "PINS error: " + name.name + " is not an array");
            else
                definitions.store(link.get(), name);
        }

        // Visiting left and right part of binary expression
        binary.left.accept(this);
        binary.right.accept(this);
    }

    @Override
    public void visit(Block block) {
        // Visiting all expressions in block
        for (Expr expression : block.expressions)
            expression.accept(this);
    }

    @Override
    public void visit(For forLoop) {
        // Visiting all parts of for loop
        forLoop.counter.accept(this);
        forLoop.low.accept(this);
        forLoop.high.accept(this);
        forLoop.step.accept(this);
        forLoop.body.accept(this);
    }

    @Override
    public void visit(Name name) {
        // Linking variable name with its definition
        Optional<Def> link = symbolTable.definitionFor(name.name);

        if (link.isEmpty())
            Report.error(name.position, "PINS error: variable " + name.name + " is not defined");
        else if (link.get() instanceof FunDef)
            Report.error(name.position, "PINS error: " + name.name + " is a function");
        else if (link.get() instanceof TypeDef)
            Report.error(name.position, "PINS error: " + name.name + " is a type");
        else
            definitions.store(link.get(), name);
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        // Visiting condition and THEN expression
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);

        // Visiting ELSE expression (if present)
        ifThenElse.elseExpression.ifPresent(expr -> expr.accept(this));
    }

    @Override
    public void visit(Literal literal) {
        return;
    }

    @Override
    public void visit(Unary unary) {
        // Visiting expression
        unary.expr.accept(this);
    }

    @Override
    public void visit(While whileLoop) {
        // Visiting condition and body of while loop
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);
    }

    @Override
    public void visit(Where where) {
        // IN NEW SCOPE
        symbolTable.inNewScope(() -> {
            where.defs.accept(this); // Visiting definitions
            where.expr.accept(this); // Visiting expression(s)
        });
        // OUT OF SCOPE
    }

    @Override
    public void visit(Defs defs) {
        // BFS (first we add all definitions in symbol table on current level)
        for (Def definition : defs.definitions) {
            if (definition instanceof VarDef varDef)
                addToSymbolTable(varDef, "PINS error: variable " + varDef.name + " is already defined");
            else if (definition instanceof TypeDef typeDef)
                addToSymbolTable(typeDef, "PINS error: type " + typeDef.name + " is already defined");
            else if (definition instanceof FunDef funDef)
                addToSymbolTable(funDef, "PINS error: function " + funDef.name + " is already defined");
            else
                Report.error(definition.position, "PINS error: unknown definition");
        }

        // DFS (second we link definitions of types, variables, functions with their usages on "sub-levels")
        for (Def definition : defs.definitions) {
            if (definition instanceof VarDef varDef)
                varDef.accept(this);
            else if (definition instanceof TypeDef typeDef)
                typeDef.accept(this);
            else if (definition instanceof FunDef funDef) {

                // Linking type of parameter with its definition
                for (Parameter parameter : funDef.parameters)
                    parameter.accept(this);

                funDef.type.accept(this);

                // Visiting function body
                symbolTable.inNewScope(() -> {
                    funDef.accept(this);
                });
            }
        }
    }

    @Override
    public void visit(FunDef funDef) {
        // Adding definitions of parameters in symbol table
        for (Parameter parameter : funDef.parameters)
            addToSymbolTable(parameter, "PINS error: parameter " + parameter.name + " is already defined");

        // Visiting function body
        funDef.body.accept(this);
    }

    @Override
    public void visit(TypeDef typeDef) {
        // Linking type of type with its definition
        if (typeDef.type instanceof TypeName typeType)
            typeType.accept(this);
        else if (typeDef.type instanceof Array typArr)
            typArr.accept(this);
    }

    @Override
    public void visit(VarDef varDef) {
        // Linking type of variable with its definitions
        if (varDef.type instanceof TypeName varType)
            varType.accept(this);
        else if (varDef.type instanceof Array varArr)
            varArr.accept(this);
    }

    @Override
    public void visit(Parameter parameter) {
        // Linking type of parameter with its definition
        if (parameter.type instanceof TypeName parType)
            parType.accept(this);
        else if (parameter.type instanceof Array parArr)
            parArr.accept(this);
    }

    @Override
    public void visit(Array array) {
        if (array.type instanceof Array arrArr)  // Multidimensional arrays
            arrArr.accept(this);
        else if (array.type instanceof TypeName arrType)  // Linking array type with its definitions
            arrType.accept(this);
    }

    @Override
    public void visit(Atom atom) {
        return;
    }

    @Override
    public void visit(TypeName name) {
        // Linking type name with its definition
        Optional<Def> link = symbolTable.definitionFor(name.identifier);

        if (link.isEmpty())
            Report.error(name.position, "PINS error: type " + name.identifier + " is not defined");
        else if (!(link.get() instanceof TypeDef))
            Report.error(name.position, "PINS error: " + name.identifier + " is not a type");
        else
            definitions.store(link.get(), name);
    }

    /*AUXILIARY METHODS*/

    private void addToSymbolTable(Def definition, String errorMessage) {
        try {
            symbolTable.insert(definition);
        } catch (DefinitionAlreadyExistsException e) {
            Report.error(definition.position, errorMessage);
        }
    }
}

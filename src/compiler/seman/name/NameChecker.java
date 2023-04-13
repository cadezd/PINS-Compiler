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
        // linking function call with its definition
        Optional<Def> link = symbolTable.definitionFor(call.name);

        if (link.isEmpty())
            Report.error(call.position, "PINS error: function " + call.name + " is not defined");
        else if (!(link.get() instanceof FunDef))
            Report.error(call.position, "PINS error: " + call.name + " is not a function");
        else
            definitions.store(link.get(), call);

        // visiting all arguments
        for (Expr arg : call.arguments)
            navigate(arg);
    }

    @Override
    public void visit(Binary binary) {
        // TODO rewrite
        if (binary.operator.equals(Binary.Operator.ARR) && binary.left instanceof Name name) {
            Optional<Def> link = symbolTable.definitionFor(name.name);

            if (link.isEmpty())
                Report.error(name.position, "PINS error: array " + name.name + " is not defined");
            else if (!(link.get() instanceof VarDef || link.get() instanceof Parameter))
                Report.error(name.position, "PINS error: " + name.name + " is not an array");
            else
                definitions.store(link.get(), name);
        }

        // visiting left part of binary expression
        navigate(binary.left);

        // visiting right part of binary expression
        navigate(binary.right);
    }

    @Override
    public void visit(Block block) {
        // visiting all expressions in block
        for (Expr expression : block.expressions)
            navigate(expression);
    }

    @Override
    public void visit(For forLoop) {
        // visiting all parts of for loop
        forLoop.counter.accept(this);
        navigate(forLoop.low);
        navigate(forLoop.high);
        navigate(forLoop.step);
        navigate(forLoop.body);
    }

    @Override
    public void visit(Name name) {
        // linking variable name with its definition
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
        // visiting condition and the expression of if expression
        navigate(ifThenElse.condition);
        navigate(ifThenElse.thenExpression);

        // visiting else expression (if present)
        if (ifThenElse.elseExpression.isPresent())
            navigate(ifThenElse.elseExpression.get());
    }

    @Override
    public void visit(Literal literal) {
        return;
    }

    @Override
    public void visit(Unary unary) {
        // visiting expression
        navigate(unary.expr);
    }

    @Override
    public void visit(While whileLoop) {
        // visiting condition and body of while loop
        navigate(whileLoop.condition);
        navigate(whileLoop.body);
    }

    @Override
    public void visit(Where where) {
        symbolTable.pushScope(); // IN NEW SCOPE
        where.defs.accept(this); // visiting definitions
        navigate(where.expr); // visiting expression(s)
        symbolTable.popScope(); // OUT OF SCOPE
    }

    @Override
    public void visit(Defs defs) {
        // BFS (first we add all definitions in symbol table)
        for (Def definition : defs.definitions) {
            if (definition instanceof VarDef varDef) {
                addToSymbolTable(varDef, "PINS error: variable " + varDef.name + " is already defined");

            } else if (definition instanceof TypeDef typeDef) {
                addToSymbolTable(typeDef, "PINS error: type " + typeDef.name + " is already defined");

            } else if (definition instanceof FunDef funDef) {
                addToSymbolTable(funDef, "PINS error: function " + funDef.name + " is already defined");

            } else {
                Report.error(definition.position, "PINS error: unknown definition");

            }
        }

        // DFS : (second we link definitions of types, variables, functions with their usages)
        for (Def definition : defs.definitions) {
            if (definition instanceof VarDef varDef) {
                varDef.accept(this);

            } else if (definition instanceof TypeDef typeDef) {
                typeDef.accept(this);

            } else if (definition instanceof FunDef funDef) {

                for (Parameter parameter : funDef.parameters)  // linking type of parameter with its definition
                    parameter.accept(this);

                if (funDef.type instanceof TypeName funType)  // linking return type with its definition
                    funType.accept(this);

                // visiting function body
                symbolTable.pushScope();
                funDef.accept(this);
                symbolTable.popScope();
            }
        }
    }

    @Override
    public void visit(FunDef funDef) {
        // adding definitions of parameters in symbol table
        for (Parameter parameter : funDef.parameters)
            addToSymbolTable(parameter, "PINS error: parameter " + parameter.name + " is already defined");

        funDef.type.accept(this);

        // visiting function body
        navigate(funDef.body);
    }

    @Override
    public void visit(TypeDef typeDef) {
        // linking type of type with its definition
        if (typeDef.type instanceof TypeName typeType)
            typeType.accept(this);
        else if (typeDef.type instanceof Array typArr)
            typArr.accept(this);
    }

    @Override
    public void visit(VarDef varDef) {
        // linking type of variable with its definitions
        if (varDef.type instanceof TypeName varType)
            varType.accept(this);
        else if (varDef.type instanceof Array varArr)
            varArr.accept(this);
    }

    @Override
    public void visit(Parameter parameter) {
        // linking type of parameter with its definition
        if (parameter.type instanceof TypeName parType)
            parType.accept(this);
        else if (parameter.type instanceof Array parArr)
            parArr.accept(this);
    }

    @Override
    public void visit(Array array) {
        if (array.type instanceof Array arrArr)  // for multidimensional arrays
            arrArr.accept(this);
        else if (array.type instanceof TypeName arrType)  // linking type of array with its definitions
            arrType.accept(this);
    }

    @Override
    public void visit(Atom atom) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(TypeName name) {
        // linking variable name with its definition
        Optional<Def> link = symbolTable.definitionFor(name.identifier);

        if (link.isEmpty())
            Report.error(name.position, "PINS error: type " + name.identifier + " is not defined");
        else if (!(link.get() instanceof TypeDef))
            Report.error(name.position, "PINS error: " + name.identifier + " is not a type");
        else
            definitions.store(link.get(), name);
    }

    /*AUXILIARY METHODS*/

    public void navigate(Expr expression) {
        // navigating thru expressions
        if (expression instanceof Call exprCall) {
            exprCall.accept(this);
        } else if (expression instanceof Binary exprBinary) {
            exprBinary.accept(this);
        } else if (expression instanceof Block exprBlock) {
            exprBlock.accept(this);
        } else if (expression instanceof For exprFor) {
            exprFor.accept(this);
        } else if (expression instanceof Name exprName) {
            exprName.accept(this);
        } else if (expression instanceof IfThenElse exprIfThenElse) {
            exprIfThenElse.accept(this);
        } else if (expression instanceof Literal exprLiteral) {
            exprLiteral.accept(this);
        } else if (expression instanceof Unary exprUnary) {
            exprUnary.accept(this);
        } else if (expression instanceof While exprWhile) {
            exprWhile.accept(this);
        } else if (expression instanceof Where exprWhere) {
            exprWhere.accept(this);
        } else {
            Report.error(expression.position, "PINS error: something went wrong when executing name checker phase");
        }
    }

    private void addToSymbolTable(Def definition, String errorMessage) {
        try {
            symbolTable.insert(definition);
        } catch (DefinitionAlreadyExistsException e) {
            Report.error(definition.position, errorMessage);
        }
    }
}

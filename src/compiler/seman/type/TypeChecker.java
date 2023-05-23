/**
 * @ Author: turk
 * @ Description: Preverjanje tipov.
 */

package compiler.seman.type;

import common.Constants;
import common.Report;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

import java.util.*;

import static common.RequireNonNull.requireNonNull;

public class TypeChecker implements Visitor {
    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč, ki jim priredimo podatkovne tipe.
     */
    private NodeDescription<Type> types;

    /**
     * Evidenca obiskanih definicij tipov (da preprečimo cikle)
     */
    private Hashtable<TypeDef, Boolean> visitedTypeDefs;

    public TypeChecker(NodeDescription<Def> definitions, NodeDescription<Type> types) {
        requireNonNull(definitions, types);
        this.definitions = definitions;
        this.types = types;
        this.visitedTypeDefs = new Hashtable<>();
    }

    @Override
    public void visit(Call call) {
        // Standard library
        if (Constants.stdLibrary.containsKey(call.name)) {
            handelStdLibrary(call);
            return;
        }

        // argument types
        for (Expr argument : call.arguments)
            argument.accept(this);

        Optional<Type> argumentType;
        List<Type> argumentTypes = new ArrayList<>();
        for (Expr argument : call.arguments) {
            argumentType = types.valueFor(argument);
            argumentType.ifPresent(argumentTypes::add);
        }

        // function type (arg1Typ, arg2Typ...) -> returnTyp
        Optional<Def> funDef = definitions.valueFor(call);
        if (funDef.isEmpty())
            return;

        Optional<Type> funType = types.valueFor(funDef.get());
        if (funType.isEmpty()) {
            funDef.get().accept(this);
            call.accept(this);
        }

        if (funType.isPresent()) {
            Optional<Type.Function> function = funType.get().asFunction();
            if (function.isEmpty())
                return;

            // we make sure that types and number of arguments match function definition
            handleWrongNumberOfArguments(call, function.get().parameters);
            handleWrongArgumentTypes(function.get(), call);

            types.store(function.get().returnType, call);
        }
    }

    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);

        Optional<Type> leftExprType = types.valueFor(binary.left);
        Optional<Type> rightExprType = types.valueFor(binary.right);

        if (leftExprType.isEmpty() || rightExprType.isEmpty())
            return;

        if (binary.operator.isAndOr()) {
            // left expression must be LOG
            if (!leftExprType.get().isLog())
                Report.error(binary.left.position, "PINS error: invalid type - expected 'log', got '" + leftExprType.get() + "'");

            // right expression must be LOG
            if (!rightExprType.get().isLog())
                Report.error(binary.left.position, "PINS error: invalid type - expected 'log', got '" + rightExprType.get() + "'");

            types.store(new Type.Atom(Type.Atom.Kind.LOG), binary);

        } else if (binary.operator.isArithmetic()) {
            // left expression must be INT
            if (!leftExprType.get().isInt())
                Report.error(binary.left.position, "PINS error: invalid type - expected 'int', got " + leftExprType.get() + "'");

            // right expression must be INT
            if (!rightExprType.get().isInt())
                Report.error(binary.left.position, "PINS error: invalid type - expected 'int', got " + rightExprType.get() + "'");

            types.store(new Type.Atom(Type.Atom.Kind.INT), binary);

        } else if (binary.operator.isComparison()) {
            // we make sure that types are same
            if (!leftExprType.get().equals(rightExprType.get()))
                Report.error(binary.position, "PINS error: operator " + binary.operator + " cannot be applied to '" + leftExprType.get() + "', '" + rightExprType.get() + "'");

            // we make sure that types are LOG or INT
            if (!leftExprType.get().isInt() && !leftExprType.get().isLog())
                Report.error(binary.position, "PINS error: operator " + binary.operator + " cannot be applied to '" + leftExprType.get() + "', '" + rightExprType.get() + "'");

            types.store(new Type.Atom(Type.Atom.Kind.LOG), binary);

        } else if (binary.operator.equals(Binary.Operator.ARR)) {
            // left expression must be ARR
            if (!leftExprType.get().isArray())
                Report.error(binary.left.position, "PINS error: invalid type - expected 'arr', got '" + rightExprType.get() + "'");

            // right expression must be INT
            if (!rightExprType.get().isInt())
                Report.error(binary.left.position, "PINS error: invalid type - expected 'int', got '" + rightExprType.get() + "'");

            Optional<Type.Array> arrType = leftExprType.get().asArray();
            arrType.ifPresent(array -> types.store(array.type, binary));

        } else if (binary.operator.equals(Binary.Operator.ASSIGN)) {
            // left and right expression must be ATOM
            if (!leftExprType.get().isAtom() || !rightExprType.get().isAtom())
                Report.error(binary.left.position, "PINS error: type must be ATOM");

            // left and right expression must be the same type
            if (!leftExprType.get().equals(rightExprType.get()))
                Report.error(binary.left.position, "PINS error: invalid type - expected '" + leftExprType.get() + "', got '" + rightExprType.get() + "'");

            types.store(leftExprType.get(), binary);
        }
    }

    @Override
    public void visit(Block block) {
        for (Expr expr : block.expressions)
            expr.accept(this);

        // last expression determines type of the block
        Expr lastExpr = block.expressions.get(block.expressions.size() - 1);
        Optional<Type> lastExprType = types.valueFor(lastExpr);
        lastExprType.ifPresent(type -> types.store(type, block));
    }

    @Override
    public void visit(For forLoop) {
        forLoop.counter.accept(this);
        forLoop.low.accept(this);
        forLoop.high.accept(this);
        forLoop.step.accept(this);
        forLoop.body.accept(this);

        Optional<Type> identifierType = types.valueFor(forLoop.counter);
        Optional<Type> lowType = types.valueFor(forLoop.low);
        Optional<Type> highType = types.valueFor(forLoop.high);
        Optional<Type> stepType = types.valueFor(forLoop.step);

        if (identifierType.isEmpty() || lowType.isEmpty() || highType.isEmpty() || stepType.isEmpty())
            return;

        if (!identifierType.get().isInt())
            Report.error(forLoop.counter.position, "PINS error: invalid type - expected 'int', got '" + identifierType.get() + "'");

        if (!lowType.get().isInt())
            Report.error(forLoop.low.position, "PINS error: invalid type - expected 'int', got '" + lowType.get() + "'");

        if (!highType.get().isInt())
            Report.error(forLoop.high.position, "PINS error: invalid type - expected 'int', got '" + highType.get() + "'");

        if (!stepType.get().isInt())
            Report.error(forLoop.step.position, "PINS error: invalid type - expected 'int', got '" + stepType.get() + "'");

        types.store(new Type.Atom(Type.Atom.Kind.VOID), forLoop);
    }

    @Override
    public void visit(Name name) {
        Optional<Def> definition = definitions.valueFor(name);
        if (definition.isEmpty())
            return;

        definition.get().accept(this);
        Optional<Type> type = types.valueFor(definition.get());
        type.ifPresent(value -> types.store(value, name));
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);
        ifThenElse.elseExpression.ifPresent(expr -> expr.accept(this));

        Optional<Type> conditionType = types.valueFor(ifThenElse.condition);
        if (conditionType.isEmpty())
            return;

        if (!conditionType.get().isLog())
            Report.error(ifThenElse.condition.position, "PINS error: invalid type - expected 'log', got '" + conditionType.get() + "'");

        types.store(new Type.Atom(Type.Atom.Kind.VOID), ifThenElse);
    }

    @Override
    public void visit(Literal literal) {
        if (literal.type == Atom.Type.INT)
            types.store(new Type.Atom(Type.Atom.Kind.INT), literal);
        else if (literal.type == Atom.Type.LOG)
            types.store(new Type.Atom(Type.Atom.Kind.LOG), literal);
        else if (literal.type == Atom.Type.STR)
            types.store(new Type.Atom(Type.Atom.Kind.STR), literal);
        else
            Report.error(literal.position, "PINS error: invalid data type");
    }

    @Override
    public void visit(Unary unary) {
        unary.expr.accept(this);

        Optional<Type> unaryExprType = types.valueFor(unary.expr);
        if (unaryExprType.isEmpty())
            return;

        // if the unary operator is NOT, expression must be LOG
        if (unary.operator.equals(Unary.Operator.NOT)) {
            if (!unaryExprType.get().isLog())
                Report.error(unary.position, "PINS error: invalid type - expected 'log', got '" + unaryExprType.get() + "'");

            types.store(unaryExprType.get(), unary);
        }

        // if the unary operator is SUB or ADD, expression must be INT
        if ((unary.operator.equals(Unary.Operator.SUB) || (unary.operator.equals(Unary.Operator.ADD)))) {
            if (!unaryExprType.get().isInt())
                Report.error(unary.position, "PINS error: invalid type - expected 'int', got '" + unaryExprType.get() + "'");

            types.store(unaryExprType.get(), unary);
        }
    }

    @Override
    public void visit(While whileLoop) {
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);

        Optional<Type> conditionType = types.valueFor(whileLoop.condition);
        if (conditionType.isEmpty())
            return;

        if (!conditionType.get().isLog())
            Report.error(whileLoop.condition.position, "PINS error: invalid type - expected 'log', got '" + conditionType.get() + "'");

        types.store(new Type.Atom(Type.Atom.Kind.VOID), whileLoop);
    }

    @Override
    public void visit(Where where) {
        where.defs.accept(this);
        where.expr.accept(this);

        // type of expression determines type od WHERE block
        Optional<Type> exprType = types.valueFor(where.expr);
        exprType.ifPresent(type -> types.store(type, where));
    }

    @Override
    public void visit(Defs defs) {
        for (Def definition : defs.definitions)
            definition.accept(this);
    }

    @Override
    public void visit(FunDef funDef) {
        // parameter types
        for (Parameter parameter : funDef.parameters)
            parameter.accept(this);

        Optional<Type> paramterType;
        List<Type> parameters = new ArrayList<>();
        for (Parameter parameter : funDef.parameters) {
            paramterType = types.valueFor(parameter);
            paramterType.ifPresent(parameters::add);
        }

        // function return type
        funDef.type.accept(this);
        Optional<Type> returnType = types.valueFor(funDef.type);
        returnType.ifPresent(type -> types.store(new Type.Function(parameters, type), funDef));

        // function body
        funDef.body.accept(this);
        Optional<Type> bodyType = types.valueFor(funDef.body);

        // we make sure that function body type and function return type match
        if (returnType.isPresent() && bodyType.isPresent())
            if (!returnType.get().equals(bodyType.get()))
                Report.error(funDef.body.position, "PINS error: function type and return type do not match - expected '" + returnType.get() + "', got '" + bodyType.get() + "'");
    }

    @Override
    public void visit(TypeDef typeDef) {
        // cycle detection (if we come across visited type twice, then it's a cycle)
        if (visitedTypeDefs.get(typeDef) != null && visitedTypeDefs.get(typeDef))
            Report.error(typeDef.position, "PINS error: cycle detected between types");

        visitedTypeDefs.put(typeDef, true);

        typeDef.type.accept(this);
        Optional<Type> type = types.valueFor(typeDef.type);
        type.ifPresent(value -> types.store(value, typeDef));

        visitedTypeDefs.put(typeDef, false);
    }

    @Override
    public void visit(VarDef varDef) {
        varDef.type.accept(this);

        Optional<Type> type = types.valueFor(varDef.type);
        type.ifPresent(value -> types.store(value, varDef));
    }

    @Override
    public void visit(Parameter parameter) {
        parameter.type.accept(this);

        Optional<Type> type = types.valueFor(parameter.type);
        type.ifPresent(value -> types.store(value, parameter));
    }

    @Override
    public void visit(Array array) {
        array.type.accept(this);

        Optional<Type> arrType = types.valueFor(array.type);
        if (arrType.isEmpty())
            return;

        Optional<Type.Atom> arrAtom = arrType.get().asAtom();
        Optional<Type.Array> arrArr = arrType.get().asArray();

        // ARR( ATOM ) - 1d array
        if (arrAtom.isPresent())
            types.store(new Type.Array(array.size, arrAtom.get()), array);

            // ARR( ARR ) - nd array
        else if (arrArr.isPresent())
            types.store(new Type.Array(array.size, arrArr.get()), array);
    }

    @Override
    public void visit(Atom atom) {
        if (atom.type == Atom.Type.INT)
            types.store(new Type.Atom(Type.Atom.Kind.INT), atom);
        else if (atom.type == Atom.Type.LOG)
            types.store(new Type.Atom(Type.Atom.Kind.LOG), atom);
        else if (atom.type == Atom.Type.STR)
            types.store(new Type.Atom(Type.Atom.Kind.STR), atom);
        else
            Report.error(atom.position, "PINS error: invalid data type");
    }

    @Override
    public void visit(TypeName name) {
        Optional<Def> definition = definitions.valueFor(name);
        if (definition.isEmpty())
            return;

        definition.get().accept(this);
        Optional<Type> type = types.valueFor(definition.get());
        type.ifPresent(value -> types.store(value, name));
    }

    /*AUXILIARY METHODS*/
    private void handelStdLibrary(Call call) {
        for (Expr argument : call.arguments)
            argument.accept(this);

        Optional<Type> argumentType;
        List<Type> argumentTypes = new ArrayList<>();
        for (Expr argument : call.arguments) {
            argumentType = types.valueFor(argument);
            argumentType.ifPresent(argumentTypes::add);
        }

        List<Type> parameters = new ArrayList<>();
        if (call.name.equals("print_str")) { // (str) -> str
            parameters.add(new Type.Atom(Type.Atom.Kind.STR));
            handleWrongNumberOfArguments(call, parameters);
            handleWrongArgumentTypes(new Type.Function(parameters, new Type.Atom(Type.Atom.Kind.STR)), call);
            types.store(new Type.Atom(Type.Atom.Kind.STR), call);

        } else if (call.name.equals("print_int")) { // (int) -> int
            parameters.add(new Type.Atom(Type.Atom.Kind.INT));
            handleWrongNumberOfArguments(call, parameters);
            handleWrongArgumentTypes(new Type.Function(parameters, new Type.Atom(Type.Atom.Kind.INT)), call);
            types.store(new Type.Atom(Type.Atom.Kind.INT), call);

        } else if (call.name.equals("print_log")) { // (log) -> log
            parameters.add(new Type.Atom(Type.Atom.Kind.LOG));
            handleWrongNumberOfArguments(call, parameters);
            handleWrongArgumentTypes(new Type.Function(parameters, new Type.Atom(Type.Atom.Kind.LOG)), call);
            types.store(new Type.Atom(Type.Atom.Kind.LOG), call);

        } else if (call.name.equals("rand_int")) { // (int, int) -> int
            parameters.add(new Type.Atom(Type.Atom.Kind.INT));
            parameters.add(new Type.Atom(Type.Atom.Kind.INT));
            handleWrongNumberOfArguments(call, parameters);
            handleWrongArgumentTypes(new Type.Function(parameters, new Type.Atom(Type.Atom.Kind.INT)), call);
            types.store(new Type.Atom(Type.Atom.Kind.INT), call);

        } else if (call.name.equals("seed")) { // (int) -> int
            parameters.add(new Type.Atom(Type.Atom.Kind.INT));
            handleWrongNumberOfArguments(call, parameters);
            handleWrongArgumentTypes(new Type.Function(parameters, new Type.Atom(Type.Atom.Kind.INT)), call);
            types.store(new Type.Atom(Type.Atom.Kind.INT), call);
        }
    }

    private void handleWrongNumberOfArguments(Call call, List<Type> parameters) {
        if (call.arguments.size() != parameters.size())
            Report.error(call.position, "PINS error: wrong number of arguments - expected " + parameters.size() + ", got " + call.arguments.size());
    }

    private void handleWrongArgumentTypes(Type.Function function, Call call) {
        boolean error = false;
        for (int i = 0; i < function.parameters.size(); i++)
            if (!types.valueFor(call.arguments.get(i)).get().equals(function.parameters.get(i)))
                error = true;

        if (!error)
            return;

        StringBuilder sb = new StringBuilder("PINS error: wrong types of arguments - expected: '");
        for (int i = 0; i < function.parameters.size(); i++)
            sb.append(function.parameters.get(i)).append("', '");
        sb.delete(sb.length() - 3, sb.length());

        sb.append(" got: '");

        for (int i = 0; i < call.arguments.size(); i++)
            sb.append(types.valueFor(call.arguments.get(i)).get()).append("', '");
        sb.delete(sb.length() - 3, sb.length());

        Report.error(call.position, sb.toString());
    }
}

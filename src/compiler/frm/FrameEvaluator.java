/**
 * @ Author: turk
 * @ Description: Analizator klicnih zapisov.
 */

package compiler.frm;

import common.Constants;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

import java.util.Optional;
import java.util.Stack;

import static common.RequireNonNull.requireNonNull;

public class FrameEvaluator implements Visitor {
    /**
     * Opis definicij funkcij in njihovih klicnih zapisov.
     */
    private NodeDescription<Frame> frames;

    /**
     * Opis definicij spremenljivk in njihovih dostopov.
     */
    private NodeDescription<Access> accesses;

    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč in njihovih podatkovnih tipov.
     */
    private final NodeDescription<Type> types;

    /**
     * Stack Builderjev
     */
    private Stack<Frame.Builder> builders = new Stack<>();

    /**
     * Trenutni static leve
     */
    private int staticLevel = 0;

    public FrameEvaluator(
            NodeDescription<Frame> frames,
            NodeDescription<Access> accesses,
            NodeDescription<Def> definitions,
            NodeDescription<Type> types
    ) {
        requireNonNull(frames, accesses, definitions, types);
        this.frames = frames;
        this.accesses = accesses;
        this.definitions = definitions;
        this.types = types;
    }

    @Override
    public void visit(Call call) {
        // Getting size of arguments
        int size = 0;
        Optional<Type> argumentType;
        for (Expr argument : call.arguments) {
            argumentType = types.valueFor(argument);
            if (argumentType.isEmpty())
                continue;
            size += argumentType.get().sizeInBytesAsParam();
        }
        // Adding size of a static link
        size += Constants.WordSize;

        // Adding space for arguments of a call to a frame
        Frame.Builder builder = builders.pop();
        builder.addFunctionCall(size);
        builders.push(builder);
    }

    @Override
    public void visit(Binary binary) {
        // Visiting all parts of binary expression
        binary.left.accept(this);
        binary.right.accept(this);
    }


    @Override
    public void visit(Block block) {
        // Visiting expressions in a block
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
    }


    @Override
    public void visit(IfThenElse ifThenElse) {
        // Visiting all parts of if then else
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);

        if (ifThenElse.elseExpression.isEmpty())
            return;

        ifThenElse.elseExpression.get().accept(this);
    }


    @Override
    public void visit(Literal literal) {
    }


    @Override
    public void visit(Unary unary) {
        unary.expr.accept(this);
    }


    @Override
    public void visit(While whileLoop) {
        // Visiting all parts of while loop
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);
    }


    @Override
    public void visit(Where where) {
        // Visiting all parts of where expression
        where.defs.accept(this);
        where.expr.accept(this);
    }


    @Override
    public void visit(Defs defs) {
        // Visiting all definitions
        for (Def definition : defs.definitions)
            definition.accept(this);
    }


    @Override
    public void visit(FunDef funDef) {
        staticLevel++;

        // First we make a Builder for frame (named or anonymous)
        Frame.Builder builder;
        if (staticLevel <= 1)
            builder = new Frame.Builder(Frame.Label.named(funDef.name), staticLevel);
        else
            builder = new Frame.Builder(Frame.Label.nextAnonymous(), staticLevel);

        // Second we add space for static link
        builder.addParameter(Constants.WordSize);
        builders.push(builder);

        // Third we visit parameters and function body
        for (Parameter parameter : funDef.parameters)
            parameter.accept(this);
        funDef.body.accept(this);

        // Last we build frame and store it
        frames.store(builder.build(), funDef);
        builders.pop();

        staticLevel--;
    }


    @Override
    public void visit(TypeDef typeDef) {
    }


    @Override
    public void visit(VarDef varDef) {
        Optional<Type> type = types.valueFor(varDef.type);
        if (type.isEmpty())
            return;


        int size = type.get().sizeInBytes();

        // Adding space in the heap for global variables
        if (staticLevel < 1) {
            String name = varDef.name;
            accesses.store(
                    new Access.Global(size, Frame.Label.named(name)),
                    varDef
            );
            return;
        }

        // Adding space in frames for local variables
        Frame.Builder builder = builders.pop();
        accesses.store(
                new Access.Local(size, builder.addLocalVariable(size), staticLevel),
                varDef
        );
        builders.push(builder);
    }


    @Override
    public void visit(Parameter parameter) {
        Optional<Type> type = types.valueFor(parameter.type);
        if (type.isEmpty())
            return;

        // Adding space in frame for parameters
        int size = type.get().sizeInBytesAsParam();
        Frame.Builder builder = builders.pop();
        accesses.store(
                new Access.Parameter(size, builder.addParameter(size), staticLevel),
                parameter
        );
        builders.push(builder);
    }


    @Override
    public void visit(Array array) {
    }


    @Override
    public void visit(Atom atom) {
    }


    @Override
    public void visit(TypeName name) {
    }
}

/**
 * @ Author: turk
 * @ Description: Visitor, ki izpiše AST.
 */

package compiler.common;

import static common.StringUtil.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import common.VoidOperator;
import compiler.parser.ast.*;
import compiler.parser.ast.def.*;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

public class PrettyPrintVisitor3 implements Visitor {
    /**
     * Trenutna indentacija.
     */
    private int indent = 0;

    /**
     * Za koliko naj se indentacija poveča pri gnezdenju.
     */
    private final int increaseIndentBy;

    /**
     * Izhodni tok, na katerega se izpiše drevo.
     */
    private PrintStream stream;

    /**
     * Razrešena imena. 
     */
    public Optional<NodeDescription<Def>> definitions = Optional.empty();

    /**
     * Razrešeni podatkovni tipi. 
     */
    public Optional<NodeDescription<Type>> types = Optional.empty();

    /**
     * Ustvari novo instanco.
     * 
     * @param increaseIndentBy za koliko naj se poveča indentacija pri gnezdenju.
     * @param stream izhodni tok.
     */
    public PrettyPrintVisitor3(int increaseIndentBy, PrintStream stream) {
        requireNonNull(stream);
        this.increaseIndentBy = increaseIndentBy;
        this.stream = stream;
    }

    /**
     * Ustvari novo instanco.
     * 
     * Privzeta vrednost `increaseIndentBy` je 4.
     * 
     * @param stream izhodni tok.
     */
    public PrettyPrintVisitor3(PrintStream stream) {
        requireNonNull(stream);
        this.increaseIndentBy = 4;
        this.stream = stream;
    }

    /**
     * Implementacija ``Visitor`` vmesnika:
     */

    @Override
    public void visit(Call call) {
        println("Call", call, call.name);
        inNewScope(() -> {
            printDefinedAt(call);
            printTypedAs(call);
            call.arguments.forEach((arg) -> arg.accept(this));
        });
    }

    @Override
    public void visit(Binary binary) {
        println("Binary", binary, binary.operator.toString());
        inNewScope(() -> {
            printTypedAs(binary);
            binary.left.accept(this);
            binary.right.accept(this);
        });
    }

    @Override
    public void visit(Block block) {
        println("Block", block);
        inNewScope(() -> {
            printTypedAs(block);
            block.expressions.forEach((expr) -> expr.accept(this));
        });
    }

    @Override
    public void visit(For forLoop) {
        println("For", forLoop);
        inNewScope(() -> {
            printTypedAs(forLoop);
            forLoop.counter.accept(this);
            forLoop.low.accept(this);
            forLoop.high.accept(this);
            forLoop.step.accept(this);
            forLoop.body.accept(this);
        });
    }

    @Override
    public void visit(Name name) {
        println("Name", name, name.name);
        inNewScope(() -> {
            printDefinedAt(name);
            printTypedAs(name);
        });
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        println("IfThenElse", ifThenElse);
        inNewScope(() -> {
            printTypedAs(ifThenElse);
            ifThenElse.condition.accept(this);
            ifThenElse.thenExpression.accept(this);
            if (ifThenElse.elseExpression.isPresent()) {
                ifThenElse.elseExpression.get().accept(this);
            }
        });
    }

    @Override
    public void visit(Literal literal) {
        println("Literal", literal, literal.type.toString(), "(", literal.value, ")");
        inNewScope(() -> printTypedAs(literal));
    }

    @Override
    public void visit(Unary unary) {
        println("Unary", unary, unary.operator.toString());
        inNewScope(() -> {
            printTypedAs(unary);
            unary.expr.accept(this);
        });
    }

    @Override
    public void visit(While whileLoop) {
        println("While", whileLoop);
        inNewScope(() -> {
            printTypedAs(whileLoop);
            whileLoop.condition.accept(this);
            whileLoop.body.accept(this);
        });
    }

    @Override
    public void visit(Where where) {
        println("Where", where);
        inNewScope(() -> {
            printTypedAs(where);
            where.defs.accept(this);
            where.expr.accept(this);
        });
    }

    // Definicije:

    @Override
    public void visit(Defs defs) {
        println("Defs", defs);
        inNewScope(() -> {
            defs.definitions.forEach((def) -> def.accept(this));
        });
    }

    @Override
    public void visit(FunDef funDef) {
        println("FunDef", funDef, funDef.name);
        inNewScope(() -> {
            printTypedAs(funDef);
            visit(funDef.parameters);
            funDef.type.accept(this);
            funDef.body.accept(this);
        });
    }

    @Override
    public void visit(TypeDef typeDef) {
        println("TypeDef", typeDef, typeDef.name);
        inNewScope(() -> {
            printTypedAs(typeDef);
            typeDef.type.accept(this);
        });
    }

    @Override
    public void visit(VarDef varDef) {
        println("VarDef", varDef, varDef.name);
        inNewScope(() -> {
            printTypedAs(varDef);
            varDef.type.accept(this);
        });
    }

    @Override
    public void visit(FunDef.Parameter parameter) {
        println("Parameter", parameter, parameter.name);
        inNewScope(() -> {
            printTypedAs(parameter);
            parameter.type.accept(this);
        });
    }

    // Tipi:

    @Override
    public void visit(Array array) {
        println("Array", array);
        inNewScope(() -> {
            printTypedAs(array);
            print("[", Integer.toString(array.size), "]\n");
            array.type.accept(this);
        });
    }

    @Override
    public void visit(Atom atom) {
        println("Atom", atom, atom.type.toString());
        inNewScope(() -> {
            printTypedAs(atom);
        });
    }

    @Override
    public void visit(TypeName name) {
        println("TypeName", name, name.identifier);
        inNewScope(() -> {
            printDefinedAt(name);
            printTypedAs(name);
        });
    }

    // ----------------------------------

    public <T extends Ast> void visit(List<T> nodes) {
        nodes.forEach((node) -> {
            node.accept(this);
        });
    }
    
    // ----------------------------------

    /**
     * Znotraj gnezdenega bloka izvede operator `op`.
     * 
     * Gnezdenje bloka poveča trenutno indentacijo,
     * izvede operator `op`, nato pa indentacijo
     * nastavi na prejšnjo vrednost.
     */
    private void inNewScope(VoidOperator op) {
        requireNonNull(op);
        indent += increaseIndentBy;
        op.apply();
        indent -= increaseIndentBy;
    }

    private void print(String... args) {
        stream.print(indented("", indent));
        for (var arg : args) {
            stream.print(arg);
        }
    }

    private void println(String line, Ast node, String... s) {
        print(line, " ", node.position.toString());
        if (s.length > 0) {
            stream.print(": ");
        }
        for (var a : s) {
            stream.print(a);
        }
        stream.println();
    }

    private void printDefinedAt(Ast node) {
        if (definitions.isPresent()) {
            var definition = definitions.get().valueFor(node);
            if (definition.isEmpty()) {
                throw new RuntimeException(node.toString());
            }
            print("# defined at: ", definition.get().position.toString(), "\n");
        }
    }

    private void printTypedAs(Ast node) {
        if (types.isPresent()) {
            var type = types.get().valueFor(node);
            if (type.isEmpty()) {
                throw new RuntimeException(node.toString());
            }
            print("# typed as: ", type.get().toString(), "\n");
        }
    }
}

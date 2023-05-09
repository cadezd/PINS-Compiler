/**
 * @ Author: turk
 * @ Description: Generator vmesne kode.
 */

package compiler.ir;

import static common.RequireNonNull.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import common.Constants;
import common.Report;
import compiler.common.Visitor;
import compiler.frm.Access;
import compiler.frm.Frame;
import compiler.frm.Frame.Label;
import compiler.ir.chunk.Chunk;
import compiler.ir.code.IRNode;
import compiler.ir.code.expr.*;
import compiler.ir.code.stmt.*;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

public class IRCodeGenerator implements Visitor {
    /**
     * Preslikava iz vozlišč AST v vmesno kodo.
     */
    private NodeDescription<IRNode> imcCode;

    /**
     * Razrešeni klicni zapisi.
     */
    private final NodeDescription<Frame> frames;

    /**
     * Razrešeni dostopi.
     */
    private final NodeDescription<Access> accesses;

    /**
     * Razrešene definicije.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Razrešeni tipi.
     */
    private final NodeDescription<Type> types;

    /**
     * **Rezultat generiranja vmesne kode** - seznam fragmentov.
     */
    public List<Chunk> chunks = new ArrayList<>();

    /**
     * Trenutni klicni zapis
     */
    private Frame currFrame;

    public IRCodeGenerator(
            NodeDescription<IRNode> imcCode,
            NodeDescription<Frame> frames,
            NodeDescription<Access> accesses,
            NodeDescription<Def> definitions,
            NodeDescription<Type> types
    ) {
        requireNonNull(imcCode, frames, accesses, definitions, types);
        this.types = types;
        this.imcCode = imcCode;
        this.frames = frames;
        this.accesses = accesses;
        this.definitions = definitions;
    }

    @Override
    public void visit(Call call) {
        // Dobimo definicijo funkcije, ki jo kličemo
        Optional<Def> funDef = definitions.valueFor(call);

        if (funDef.isEmpty())
            return;

        // Dobimo frame funkcije, ki jo kličemo
        Optional<Frame> funFrame = frames.valueFor(funDef.get());

        if (funFrame.isEmpty())
            return;

        // Grajenje drevesa pri ESEQ
        NameExpr sp = NameExpr.SP();
        ConstantExpr offset = new ConstantExpr(currFrame.oldFPOffset());
        BinopExpr binopExpr = new BinopExpr(sp, offset, BinopExpr.Operator.SUB);

        MemExpr memExpr = new MemExpr(binopExpr);
        NameExpr fp = NameExpr.FP();
        MoveStmt moveStmt = new MoveStmt(memExpr, fp);


        // Pretvorba argumentov in klica v vmesno kodo za klic
        Optional<IRNode> irNode;
        List<IRExpr> args = new ArrayList<>();

        for (Expr expression : call.arguments) {
            expression.accept(this);
            irNode = imcCode.valueFor(expression);
            if (irNode.isEmpty())
                continue;
            args.add((IRExpr) irNode.get());
        }

        CallExpr callExpr = new CallExpr(funFrame.get().label, args);

        // Shranjevanje ESEQ stavka
        EseqExpr eseqExpr = new EseqExpr(moveStmt, callExpr);
        imcCode.store(eseqExpr, call);
    }

    @Override
    public void visit(Binary binary) {
        // Obdelamo levo in desno stran izraza
        binary.left.accept(this);
        binary.right.accept(this);

        // Pridobimo obdelano levo in desno stran izraza
        Optional<IRNode> leftExpr = imcCode.valueFor(binary.left);
        Optional<IRNode> rightExpr = imcCode.valueFor(binary.right);

        if (leftExpr.isEmpty() || rightExpr.isEmpty())
            return;

        // Izberemo pravilni operator izraza
        BinopExpr.Operator operator;

        if (binary.operator == Binary.Operator.ASSIGN) {
            MoveStmt moveStmt = new MoveStmt((IRExpr) leftExpr.get(), (IRExpr) rightExpr.get());
            EseqExpr eseqExpr = new EseqExpr(moveStmt, (IRExpr) leftExpr.get());
            imcCode.store(eseqExpr, binary);

        } else if (binary.operator == Binary.Operator.ARR) {
            Optional<Type> arrType = types.valueFor(binary);
            if (arrType.isEmpty())
                return;

            BinopExpr index = new BinopExpr(    // index elementa  (idx * velikost tipa)
                    (IRExpr) rightExpr.get(),
                    new ConstantExpr(arrType.get().sizeInBytes()),
                    BinopExpr.Operator.ADD);

            if (!(leftExpr.get() instanceof MemExpr))
                return;

            BinopExpr arrElement = new BinopExpr(   // naslov tabele + index elementa
                    ((MemExpr) leftExpr.get()).expr,
                    index,
                    BinopExpr.Operator.ADD);

            imcCode.store(new MemExpr(arrElement), binary);

        } else {
            operator = BinopExpr.Operator.valueOf(binary.operator.name());
            imcCode.store(
                    new BinopExpr(
                            (IRExpr) leftExpr.get(),
                            (IRExpr) rightExpr.get(),
                            operator),
                    binary
            );
        }
    }

    @Override
    public void visit(Block block) {
        List<IRStmt> statements = new ArrayList<>();
        Optional<IRNode> node;

        for (Expr expression : block.expressions) {
            // Obdelamo izraz
            expression.accept(this);

            // Naredimo stavke iz bloka
            node = imcCode.valueFor(expression);
            if (node.isEmpty())
                continue;

            // Če je stavek ga dodaj, če je izraz ga pretvori v stavek
            statements.add((node.get() instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) node.get());
        }

        node = imcCode.valueFor(block.expressions.get(block.expressions.size() - 1));
        if (node.isEmpty())
            return;

        statements.remove(statements.size() - 1);
        imcCode.store(new EseqExpr(
                new SeqStmt(statements),
                (IRExpr) node.get()
        ), block);
    }

    @Override
    public void visit(For forLoop) {
        // Obdelamo vse komponente for stavka
        forLoop.counter.accept(this);
        forLoop.low.accept(this);
        forLoop.high.accept(this);
        forLoop.step.accept(this);
        forLoop.body.accept(this);

        // Ustvarimo labele
        LabelStmt l1 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l2 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l3 = new LabelStmt(Label.nextAnonymous());

        // Vmesna koda za counter (counter = low)
        Optional<IRNode> counter = imcCode.valueFor(forLoop.counter);
        Optional<IRNode> low = imcCode.valueFor(forLoop.low);
        if (counter.isEmpty() || low.isEmpty())
            return;

        MoveStmt moveCounter = new MoveStmt((IRExpr) counter.get(), (IRExpr) low.get());

        // Vmesna koda za pogoj (counter < high)
        Optional<IRNode> high = imcCode.valueFor(forLoop.high);
        if (high.isEmpty())
            return;

        BinopExpr condition = new BinopExpr(
                (IRExpr) low.get(),
                (IRExpr) high.get(),
                BinopExpr.Operator.LT);

        CJumpStmt cJumpStmtL1L2 = new CJumpStmt(condition, l1.label, l2.label);

        // Vmesna koda telesa for zanke
        Optional<IRNode> body = imcCode.valueFor(forLoop.body);
        if (body.isEmpty())
            return;

        // Vmesna koda za povečanje števca zanke (counter = counter + step)
        Optional<IRNode> step = imcCode.valueFor(forLoop.step);
        if (step.isEmpty())
            return;

        BinopExpr incrementCounter = new BinopExpr(
                (IRExpr) counter.get(),
                (IRExpr) step.get(),
                BinopExpr.Operator.ADD);

        MoveStmt updateCounter = new MoveStmt((IRExpr) counter.get(), incrementCounter);

        // Vmesna koda za skok na pogoj
        JumpStmt jumpStmtL3 = new JumpStmt(l3.label);


        // Shranimo vmesno kodo
        List<IRStmt> statements = new ArrayList<>();
        statements.add(moveCounter);
        statements.add(l3);
        statements.add(cJumpStmtL1L2);
        statements.add(l1);
        statements.add((body.get() instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) body.get());
        statements.add(updateCounter);
        statements.add(jumpStmtL3);
        statements.add(l2);

        imcCode.store(new EseqExpr(new SeqStmt(statements), new ConstantExpr(0)), forLoop);
    }

    @Override
    public void visit(Name name) {
        // Dobimo definicijo spremenljivke
        Optional<Def> nameDef = definitions.valueFor(name);
        if (nameDef.isEmpty())
            return;

        // Dobimo dostop do spremenljivke
        Optional<Access> nameAccess = accesses.valueFor(nameDef.get());
        if (nameAccess.isEmpty())
            return;

        // Hendlamo spremenljivko glede na dostop
        if (nameAccess.get() instanceof Access.Global global) {

            NameExpr nameExpr = new NameExpr(global.label);
            MemExpr memExpr = new MemExpr(nameExpr);
            imcCode.store(memExpr, name);

        } else if (nameAccess.get() instanceof Access.Parameter parameter) {

            NameExpr nameExpr = NameExpr.FP();
            ConstantExpr constantExpr = new ConstantExpr(parameter.offset);
            BinopExpr binopExpr = new BinopExpr(nameExpr, constantExpr, BinopExpr.Operator.ADD);
            MemExpr memExpr = new MemExpr(binopExpr);
            imcCode.store(memExpr, name);

        } else if (nameAccess.get() instanceof Access.Local local) {

            // Dodajamo mem-e
            int diff = Math.abs(currFrame.staticLevel - local.staticLevel);

            if (diff == 0) {
                ConstantExpr constantExpr = new ConstantExpr(local.offset);
                NameExpr nameExpr = NameExpr.FP();
                BinopExpr binopExpr = new BinopExpr(nameExpr, constantExpr, BinopExpr.Operator.ADD);
                imcCode.store(binopExpr, name);

            } else {
                NameExpr nameExpr = NameExpr.FP();
                MemExpr memExpr = null;
                for (int i = 0; i < diff; i++) {
                    memExpr = new MemExpr(nameExpr);
                }

                ConstantExpr constantExpr = new ConstantExpr(local.offset);
                BinopExpr binopExpr = new BinopExpr(memExpr, constantExpr, BinopExpr.Operator.ADD);
                imcCode.store(binopExpr, name);
            }
        }
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        // Obdelamo vse komponente ifThen stavka
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);

        List<IRStmt> statements = new ArrayList<>();

        // Ustvarimo labele
        LabelStmt l1 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l2 = new LabelStmt(Label.nextAnonymous());

        // Vmesna koda pogoja
        Optional<IRNode> condition = imcCode.valueFor(ifThenElse.condition);
        if (condition.isEmpty())
            return;

        CJumpStmt cJumpStmtL1L2 = new CJumpStmt((IRExpr) condition.get(), l1.label, l2.label);

        // Vmesna koda then
        Optional<IRNode> then = imcCode.valueFor(ifThenElse.thenExpression);
        if (then.isEmpty())
            return;


        statements.add(cJumpStmtL1L2);
        statements.add(l1);
        statements.add((then.get() instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) then.get());
        statements.add(l2);

        // Vmesna koda else
        if (ifThenElse.elseExpression.isPresent()) {
            ifThenElse.elseExpression.get().accept(this);

            LabelStmt l3 = new LabelStmt(Label.nextAnonymous());
            JumpStmt jumL3 = new JumpStmt(l3.label);

            Optional<IRNode> else1 = imcCode.valueFor(ifThenElse.elseExpression.get());
            if (else1.isEmpty())
                return;

            statements.add(3, jumL3);
            statements.add((else1.get() instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) else1.get());
            statements.add(l3);
        }

        imcCode.store(new EseqExpr(new SeqStmt(statements), new ConstantExpr(0)), ifThenElse);
    }

    @Override
    public void visit(Literal literal) {
        // Globalno shranimo string z labelo
        if (literal.type == Atom.Type.STR) {
            Label l = Label.nextAnonymous();
            accesses.store(new Access.Global(Constants.WordSize, l), literal);
            Optional<Access> access = accesses.valueFor(literal);
            if (access.isEmpty())
                return;

            chunks.add(new Chunk.DataChunk((Access.Global) access.get(), literal.value));
            imcCode.store(new NameExpr(l), literal);
            return;
        }

        // Shranimo vrednost
        int value;
        if (literal.type == Atom.Type.LOG)
            value = (literal.value.equals("true")) ? 1 : 0;
        else
            value = Integer.parseInt(literal.value);

        imcCode.store(new ConstantExpr(value), literal);
    }

    @Override
    public void visit(Unary unary) {
        // Obdelamo izraz unarnega operatorja
        unary.expr.accept(this);

        Optional<IRNode> expr = imcCode.valueFor(unary.expr);
        if (expr.isEmpty())
            return;

        if (unary.operator == Unary.Operator.ADD) // Če je predznak plus pustimo konstanto
            imcCode.store(expr.get(), unary);
        else if (unary.operator == Unary.Operator.SUB) // Če je predznak minus zapišemo kot 0 - vrednost
            imcCode.store(new BinopExpr(
                            new ConstantExpr(0),
                            (IRExpr) expr.get(),
                            BinopExpr.Operator.SUB
                    ),
                    unary);
        else { // Če je predznak negacija zapišemo obrnemo vrednost iz 0 v 1 ali obratno
            ConstantExpr constantExprNegated = new ConstantExpr((((ConstantExpr) expr.get()).constant == 1) ? 0 : 1);
            imcCode.store(constantExprNegated, unary);
        }
    }

    @Override
    public void visit(While whileLoop) {
        // Obdelamo vse komponente while stavka
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);

        // Ustvarimo labele
        LabelStmt l1 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l2 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l3 = new LabelStmt(Label.nextAnonymous());

        // Vmesna koda za pogoj
        Optional<IRNode> condition = imcCode.valueFor(whileLoop.condition);
        if (condition.isEmpty())
            return;

        CJumpStmt cJumpStmtL1L2 = new CJumpStmt(
                (IRExpr) condition.get(),
                l1.label,
                l2.label
        );

        // Vmesna koda telesa while stavka
        Optional<IRNode> body = imcCode.valueFor(whileLoop.body);
        if (body.isEmpty())
            return;

        // Vmesna koda za labelo, ki skoči na začetek zanke
        JumpStmt jumpL3 = new JumpStmt(l3.label);


        List<IRStmt> statements = new ArrayList<>();
        statements.add(l3);
        statements.add(cJumpStmtL1L2);
        statements.add(l1);
        statements.add((body.get() instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) body.get());
        statements.add(jumpL3);
        statements.add(l2);

        imcCode.store(new EseqExpr(new SeqStmt(statements), new ConstantExpr(0)), whileLoop);
    }

    @Override
    public void visit(Where where) {
        // Obdelamo definicije in expressione
        where.defs.accept(this);
        where.expr.accept(this);

        // Shranimo vmesno kodo za izraze iz where bloka
        Optional<IRNode> expr = imcCode.valueFor(where.expr);
        if (expr.isEmpty())
            return;

        imcCode.store(expr.get(), where);
    }

    @Override
    public void visit(Defs defs) {
        // Obdelamo definicije
        for (Def definition : defs.definitions)
            definition.accept(this);
    }

    @Override
    public void visit(FunDef funDef) {
        // Pridobimo frame funkcije
        Optional<Frame> funFrame = frames.valueFor(funDef);
        if (funFrame.isEmpty())
            return;

        // Si zapomnimo trenutno funkcijo in jo obdelamo (lokalne sprem. v drugih funkcijah)
        Frame oldFrame = currFrame;
        currFrame = funFrame.get();
        funDef.body.accept(this);

        // Pridobimo vmesno kodo funkcije
        Optional<IRNode> funCode = imcCode.valueFor(funDef.body);
        if (funCode.isEmpty())
            return;

        // naredimo še MOVE del in vmesno kodo shranimo
        NameExpr nameExpr = NameExpr.FP();
        MemExpr memExpr = new MemExpr(nameExpr);
        MoveStmt moveStmt = new MoveStmt(memExpr, (IRExpr) funCode.get());
        chunks.add(new Chunk.CodeChunk(funFrame.get(), moveStmt));

        currFrame = oldFrame;
    }

    @Override
    public void visit(TypeDef typeDef) {
    }

    @Override
    public void visit(VarDef varDef) {
        Optional<Access> varAccess = accesses.valueFor(varDef);
        if (varAccess.isEmpty())
            return;

        if (varAccess.get() instanceof Access.Global global)
            chunks.add(new Chunk.GlobalChunk(global));
    }

    @Override
    public void visit(Parameter parameter) {
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

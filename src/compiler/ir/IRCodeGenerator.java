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
import compiler.parser.ast.Ast;
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
        // Standardna knjižnica
        if (Constants.stdLibrary.containsKey(call.name)) {
            handleStdLibrary(call);
            return;
        }

        // Dobimo definicijo funkcije, ki jo kličemo
        Def funDef = getDef(call);
        // Dobimo frame funkcije, ki jo kličemo
        Frame funFrame = getFrame(funDef);

        // Grajenje drevesa pri ESEQ
        NameExpr stackPointer = NameExpr.SP();
        ConstantExpr offset = new ConstantExpr(currFrame.oldFPOffset());
        BinopExpr dstAddress = new BinopExpr(stackPointer, offset, BinopExpr.Operator.SUB);

        MemExpr dstAddress1 = new MemExpr(dstAddress);
        NameExpr framePointer = NameExpr.FP();
        MoveStmt returnValue = new MoveStmt(dstAddress1, framePointer);


        // Dobimo static link
        IRExpr staticLink;
        int diff = currFrame.staticLevel - funFrame.staticLevel;
        if (diff == -1)
            staticLink = NameExpr.FP();
        else {
            staticLink = new MemExpr(NameExpr.FP());
            for (int i = 0; i < diff; i++)
                staticLink = new MemExpr(staticLink);
        }

        // Dodamo argumente
        List<IRExpr> arguments = new ArrayList<>();
        if (funFrame.staticLevel == 1) arguments.add(new ConstantExpr(-1));
        else arguments.add(staticLink);

        for (Expr argument : call.arguments) {
            arguments.add((IRExpr) getIRNode(argument));
        }

        CallExpr callExpr = new CallExpr(funFrame.label, arguments);
        // Shranjevanje ESEQ stavka
        EseqExpr eseqExpr = new EseqExpr(returnValue, callExpr);
        imcCode.store(eseqExpr, call);
    }

    @Override
    public void visit(Binary binary) {
        // Pridobimo obdelano levo in desno stran izraza
        IRNode leftExpr = getIRNode(binary.left);
        IRNode rightExpr = getIRNode(binary.right);

        if (binary.operator == Binary.Operator.ASSIGN) {
            MoveStmt moveStmt = new MoveStmt((IRExpr) leftExpr, (IRExpr) rightExpr);
            EseqExpr eseqExpr = new EseqExpr(moveStmt, (IRExpr) leftExpr);
            imcCode.store(eseqExpr, binary);

        } else if (binary.operator == Binary.Operator.ARR) {
            Type arrType = getType(binary);

            // index elementa  (idx * velikost tipa)
            BinopExpr index = new BinopExpr(
                    (IRExpr) rightExpr,
                    new ConstantExpr(arrType.sizeInBytes()),
                    BinopExpr.Operator.MUL
            );

            // naslov elementa (arr_address + index_elementa)
            BinopExpr arrayElementAddress = new BinopExpr(
                    (IRExpr) leftExpr,
                    index,
                    BinopExpr.Operator.ADD
            );
            MemExpr arrayElementValue = new MemExpr(arrayElementAddress);

            imcCode.store(
                    (arrType.isArray()) ?
                            arrayElementAddress :
                            arrayElementValue,
                    binary
            );

        } else {
            BinopExpr.Operator operator = BinopExpr.Operator.valueOf(binary.operator.name());
            imcCode.store(
                    new BinopExpr((IRExpr) leftExpr, (IRExpr) rightExpr, operator),
                    binary
            );
        }
    }

    @Override
    public void visit(Block block) {
        // Iz n-1 stavkov naredimo blok, zadnji stavek je tip bloka
        List<IRStmt> statements = new ArrayList<>();
        IRNode irNode;
        for (Expr expression : block.expressions) {
            irNode = getIRNode(expression);
            statements.add((irNode instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) irNode);
        }

        IRNode lastIrNode = getIRNode(block.expressions.get(block.expressions.size() - 1));
        statements.remove(statements.size() - 1);

        imcCode.store(
                new EseqExpr(new SeqStmt(statements), (IRExpr) lastIrNode),
                block
        );
    }

    @Override
    public void visit(For forLoop) {
        // Ustvarimo labele
        LabelStmt l1 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l2 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l3 = new LabelStmt(Label.nextAnonymous());


        // Vmesna koda za counter (counter = low)
        IRNode counter = getIRNode(forLoop.counter);
        IRNode low = getIRNode(forLoop.low);
        MoveStmt moveCounter = new MoveStmt((IRExpr) counter, (IRExpr) low);


        // Vmesna koda za pogoj (counter < high)
        IRNode high = getIRNode(forLoop.high);
        BinopExpr condition = new BinopExpr((IRExpr) counter, (IRExpr) high, BinopExpr.Operator.LT);
        CJumpStmt cJumpStmtL1L2 = new CJumpStmt(condition, l1.label, l2.label);


        // Vmesna koda telesa for zanke
        IRNode body = getIRNode(forLoop.body);


        // Vmesna koda za povečanje števca zanke (counter = counter + step)
        IRNode step = getIRNode(forLoop.step);
        BinopExpr incrementCounter = new BinopExpr((IRExpr) counter, (IRExpr) step, BinopExpr.Operator.ADD);
        MoveStmt updateCounter = new MoveStmt((IRExpr) counter, incrementCounter);

        // Vmesna koda za skok na pogoj
        JumpStmt jumpStmtL3 = new JumpStmt(l3.label);

        // Shranimo vmesno kodo (MOVE COUNTER, L3, CJUMP (L1,L2), L1, BODY, UPDATE COUNTER, JMP L3, L2)
        List<IRStmt> statements = new ArrayList<>();
        statements.add(moveCounter);
        statements.add(l3);
        statements.add(cJumpStmtL1L2);
        statements.add(l1);
        statements.add((body instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) body);
        statements.add(updateCounter);
        statements.add(jumpStmtL3);
        statements.add(l2);

        imcCode.store(new EseqExpr(new SeqStmt(statements), new ConstantExpr(0)), forLoop);
    }

    @Override
    public void visit(Name name) {
        // Dobimo definicijo spremenljivke
        Def nameDef = getDef(name);
        // Dobimo dostop do spremenljivke
        Access nameAccess = getAccess(nameDef);
        // Dobimo tip spremenljivke
        Type nameType = getType(name);

        // Hendlamo spremenljivko glede na dostop
        if (nameAccess instanceof Access.Global global) {

            NameExpr nameExpr = new NameExpr(global.label);
            imcCode.store(
                    (nameType.isArray()) ? nameExpr : new MemExpr(nameExpr),
                    name
            );

        } else if (nameAccess instanceof Access.Parameter parameter) {

            int diff = Math.abs(currFrame.staticLevel - parameter.staticLevel);
            if (diff == 0) {
                NameExpr framePointer = NameExpr.FP();
                ConstantExpr paramOffset = new ConstantExpr(parameter.offset);
                BinopExpr paramAddress = new BinopExpr(framePointer, paramOffset, BinopExpr.Operator.ADD);
                MemExpr paramValue = new MemExpr(paramAddress);

                imcCode.store(paramValue, name);

            } else {
                // Dodajamo mem-e (od 1 naprej -> new MemExpr(nameExpr))
                NameExpr framePointer = NameExpr.FP();
                MemExpr memExpr = new MemExpr(framePointer);
                for (int i = 1; i < diff; i++)
                    memExpr = new MemExpr(memExpr);

                ConstantExpr paramOffset = new ConstantExpr(parameter.offset);
                BinopExpr paramAddress = new BinopExpr(memExpr, paramOffset, BinopExpr.Operator.ADD);
                MemExpr paramValue = new MemExpr(paramAddress);

                imcCode.store(paramValue, name);
            }

        } else if (nameAccess instanceof Access.Local local) {

            int diff = Math.abs(currFrame.staticLevel - local.staticLevel);
            if (diff == 0) {
                NameExpr framePointer = NameExpr.FP();
                ConstantExpr locOffset = new ConstantExpr(local.offset);
                BinopExpr locAddress = new BinopExpr(framePointer, locOffset, BinopExpr.Operator.ADD);
                MemExpr locValue = new MemExpr(locAddress);

                imcCode.store(
                        (nameType.isArray()) ? locAddress : locValue,
                        name
                );

            } else {
                // Dodajamo mem-e (od 1 naprej -> new MemExpr(nameExpr))
                NameExpr framePointer = NameExpr.FP();
                MemExpr memExpr = new MemExpr(framePointer);
                for (int i = 1; i < diff; i++)
                    memExpr = new MemExpr(memExpr);

                ConstantExpr locOffset = new ConstantExpr(local.offset);
                BinopExpr locAddress = new BinopExpr(memExpr, locOffset, BinopExpr.Operator.ADD);
                MemExpr locValue = new MemExpr(locAddress);

                imcCode.store(
                        (nameType.isArray()) ? locAddress : locValue,
                        name
                );
            }
        }
    }

    @Override
    public void visit(IfThenElse ifThenElse) {

        List<IRStmt> statements = new ArrayList<>();

        // Ustvarimo labele
        LabelStmt l1 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l2 = new LabelStmt(Label.nextAnonymous());

        // Vmesna koda pogoja
        IRNode condition = getIRNode(ifThenElse.condition);
        CJumpStmt cJumpStmtL1L2 = new CJumpStmt((IRExpr) condition, l1.label, l2.label);

        // Vmesna koda then
        IRNode then = getIRNode(ifThenElse.thenExpression);

        // CJUMP(L1, L2) L1 STMT L2
        statements.add(cJumpStmtL1L2);
        statements.add(l1);
        statements.add((then instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) then);
        statements.add(l2);

        // Vmesna koda else
        // CJUMP(L1, L2) L1 STMT JUMP(L3) L2 STMT L3
        if (ifThenElse.elseExpression.isPresent()) {
            IRNode else1 = getIRNode(ifThenElse.elseExpression.get());

            LabelStmt l3 = new LabelStmt(Label.nextAnonymous());
            JumpStmt jumL3 = new JumpStmt(l3.label);

            statements.add(3, jumL3);
            statements.add((else1 instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) else1);
            statements.add(l3);
        }

        imcCode.store(new EseqExpr(new SeqStmt(statements), new ConstantExpr(0)), ifThenElse);
    }

    @Override
    public void visit(Literal literal) {
        // Globalno shranimo string z labelo
        if (literal.type == Atom.Type.STR) {
            Label label = Label.nextAnonymous();
            accesses.store(new Access.Global(Constants.WordSize, label), literal);
            Access access = getAccess(literal);
            chunks.add(new Chunk.DataChunk((Access.Global) access, literal.value));
            imcCode.store(new NameExpr(label), literal);
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
        IRNode expression = getIRNode(unary.expr);

        if (unary.operator == Unary.Operator.ADD) // Če je predznak plus pustimo konstanto
            imcCode.store(expression, unary);
        else if (unary.operator == Unary.Operator.SUB) { // Če je predznak minus zapišemo kot (CONST(0) - CONST(value))
            BinopExpr negative = new BinopExpr(new ConstantExpr(0), (IRExpr) expression, BinopExpr.Operator.SUB);
            imcCode.store(negative, unary);
        } else { // Če je predznak negacija zapišemo obrnemo vrednost iz 0 v 1 ali obratno (1 - input)
            BinopExpr negated = new BinopExpr(new ConstantExpr(1), (IRExpr) expression, BinopExpr.Operator.SUB);
            imcCode.store(negated, unary);
        }
    }

    @Override
    public void visit(While whileLoop) {
        // Ustvarimo labele
        LabelStmt l1 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l2 = new LabelStmt(Label.nextAnonymous());
        LabelStmt l3 = new LabelStmt(Label.nextAnonymous());

        // Vmesna koda za pogoj
        IRNode condition = getIRNode(whileLoop.condition);
        CJumpStmt cJumpStmtL1L2 = new CJumpStmt((IRExpr) condition, l1.label, l2.label);

        // Vmesna koda telesa while stavka
        IRNode body = getIRNode(whileLoop.body);

        // Vmesna koda za labelo, ki skoči na začetek zanke
        JumpStmt jumpL3 = new JumpStmt(l3.label);

        // Shranimo vmesno kodo za while zanko (L3, CJUMP (L1,L2), L1, BODY, JUMP L3, L2)
        List<IRStmt> statements = new ArrayList<>();
        statements.add(l3);
        statements.add(cJumpStmtL1L2);
        statements.add(l1);
        statements.add((body instanceof IRExpr expr) ? new ExpStmt(expr) : (IRStmt) body);
        statements.add(jumpL3);
        statements.add(l2);

        imcCode.store(new EseqExpr(new SeqStmt(statements), new ConstantExpr(0)), whileLoop);
    }

    @Override
    public void visit(Where where) {
        // Obdelamo definicije
        where.defs.accept(this);

        // Shranimo vmesno kodo za izraze iz where bloka
        IRNode expr = getIRNode(where.expr);
        imcCode.store(expr, where);
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
        Frame funFrame = getFrame(funDef);

        // Si zapomnimo trenutno funkcijo in jo obdelamo (lokalne sprem. v drugih funkcijah)
        Frame oldFrame = currFrame;
        currFrame = funFrame;

        // Pridobimo vmesno kodo funkcije
        IRNode funCode = getIRNode(funDef.body);

        // naredimo še MOVE del in vmesno kodo shranimo
        NameExpr framePointer = NameExpr.FP();
        MemExpr fpValue = new MemExpr(framePointer);
        MoveStmt moveStmt = new MoveStmt(fpValue, (IRExpr) funCode);
        chunks.add(new Chunk.CodeChunk(funFrame, moveStmt));

        currFrame = oldFrame;
    }

    @Override
    public void visit(TypeDef typeDef) {
    }

    @Override
    public void visit(VarDef varDef) {
        Access varAccess = getAccess(varDef);

        if (varAccess instanceof Access.Global global)
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

    /*AUXILIARY METHODS*/
    private void handleStdLibrary(Call call) {
        List<IRExpr> arguments = new ArrayList<>();
        arguments.add(NameExpr.FP());

        for (Expr argument : call.arguments)
            arguments.add((IRExpr) getIRNode(argument));

        imcCode.store(
                new CallExpr(Label.named(call.name), arguments),
                call
        );
    }

    private IRNode getIRNode(Ast ast) {
        ast.accept(this);
        Optional<IRNode> irNode = imcCode.valueFor(ast);
        if (irNode.isEmpty()) {
            Report.error(ast.position, "PINS error: missing IRNode");
            return null;
        }

        return irNode.get();
    }

    private Def getDef(Ast ast) {
        Optional<Def> def = definitions.valueFor(ast);
        if (def.isEmpty()) {
            Report.error(ast.position, "PINS error: missing definition");
            return null;
        }

        return def.get();
    }

    private Frame getFrame(Ast ast) {
        Optional<Frame> frame = frames.valueFor(ast);
        if (frame.isEmpty()) {
            Report.error(ast.position, "PINS error: missing frame");
            return null;
        }

        return frame.get();
    }

    private Type getType(Ast ast) {
        Optional<Type> type = types.valueFor(ast);
        if (type.isEmpty()) {
            Report.error(ast.position, "PINS error: missing type");
            return null;
        }

        return type.get();
    }

    private Access getAccess(Ast ast) {
        Optional<Access> access = accesses.valueFor(ast);
        if (access.isEmpty()) {
            Report.error(ast.position, "PINS error: missing access");
            return null;
        }

        return access.get();
    }
}
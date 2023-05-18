/**
 * @ Author: turk
 * @ Description: Navidezni stroj (intepreter).
 */

package compiler.interpret;

import static common.RequireNonNull.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;

import common.Constants;
import compiler.frm.Frame;
import compiler.gen.Memory;
import compiler.gen.Memory;
import compiler.ir.chunk.Chunk.CodeChunk;
import compiler.ir.code.IRNode;
import compiler.ir.code.expr.*;
import compiler.ir.code.stmt.*;
import compiler.ir.IRPrettyPrint;

public class Interpreter {
    /**
     * Pomnilnik navideznega stroja.
     */
    private Memory memory;

    /**
     * Izhodni tok, kamor izpisujemo rezultate izvajanja programa.
     * <p>
     * V primeru, da rezultatov ne želimo izpisovati, nastavimo na `Optional.empty()`.
     */
    private Optional<PrintStream> outputStream;

    /**
     * Generator naključnih števil.
     */
    private Random random;

    /**
     * Skladovni kazalec (kaže na dno sklada).
     */
    private int stackPointer;

    /**
     * Klicni kazalec (kaže na vrh aktivnega klicnega zapisa).
     */
    private int framePointer;

    public Interpreter(Memory memory, Optional<PrintStream> outputStream) {
        requireNonNull(memory, outputStream);
        this.memory = memory;
        this.outputStream = outputStream;
        this.stackPointer = memory.size - Constants.WordSize;
        this.framePointer = memory.size - Constants.WordSize;
    }

    // --------- izvajanje navideznega stroja ----------

    public void interpret(CodeChunk chunk) {
        memory.stM(framePointer + Constants.WordSize, 0); // argument v funkcijo main
        memory.stM(framePointer - chunk.frame.oldFPOffset(), framePointer); // oldFP
        internalInterpret(chunk, new HashMap<>());
    }

    private void internalInterpret(CodeChunk chunk, Map<Frame.Temp, Object> temps) {
        // Nastavi FP in SP na nove vrednosti
        int oldFP = framePointer;
        int odlSP = stackPointer;
        framePointer = stackPointer;
        stackPointer -= chunk.frame.size();

        Object result = null;
        if (chunk.code instanceof SeqStmt seq) {
            for (int pc = 0; pc < seq.statements.size(); pc++) {
                var stmt = seq.statements.get(pc);
                result = execute(stmt, temps);
                if (result instanceof Frame.Label label) {
                    for (int q = 0; q < seq.statements.size(); q++) {
                        if (seq.statements.get(q) instanceof LabelStmt labelStmt && labelStmt.label.equals(label)) {
                            pc = q;
                            break;
                        }
                    }
                }
            }
        } else {
            throw new RuntimeException("Linearize IR!");
        }

        // Ponastavi FP in SP na stare vrednosti
        framePointer = oldFP;
        stackPointer = odlSP;
    }

    private Object execute(IRStmt stmt, Map<Frame.Temp, Object> temps) {
        if (stmt instanceof CJumpStmt cjump) {
            return execute(cjump, temps);
        } else if (stmt instanceof ExpStmt exp) {
            return execute(exp, temps);
        } else if (stmt instanceof JumpStmt jump) {
            return execute(jump, temps);
        } else if (stmt instanceof LabelStmt label) {
            return null;
        } else if (stmt instanceof MoveStmt move) {
            return execute(move, temps);
        } else {
            throw new RuntimeException("Cannot execute this statement!");
        }
    }

    private Object execute(CJumpStmt cjump, Map<Frame.Temp, Object> temps) {
        boolean condition = toBool(execute(cjump.condition, temps));    // Vrednost pogoja
        return (condition) ?
                execute(new JumpStmt(cjump.thenLabel), temps) :         // Če je TRUE -> skoči na THEN
                execute(new JumpStmt(cjump.elseLabel), temps);          // Če je FALSE -> skoči na ELSE
    }

    private Object execute(ExpStmt exp, Map<Frame.Temp, Object> temps) {
        return execute(exp.expr, temps);
    }

    private Object execute(JumpStmt jump, Map<Frame.Temp, Object> temps) {
        return jump.label;
    }

    private Object execute(MoveStmt move, Map<Frame.Temp, Object> temps) {
        Object object = execute(move.src, temps);
        Integer value = (object == null) ? 0 : toInt(object);    // Vredenost

        if (move.dst instanceof TempExpr tempExpr) {
            temps.put(tempExpr.temp, value);
            return temps.get(tempExpr.temp);
        }

        int address = (move.dst instanceof MemExpr memExpr) ?
                toInt(execute(memExpr.expr, temps)) :   // Če je MEM ga preskoči (da shrani naslov, ne pa vrednost)
                toInt(execute(move.dst, temps));        // Če je naslov ga shrani

        memory.stM(address, value);
        return memory.ldM(address);
    }

    private Object execute(IRExpr expr, Map<Frame.Temp, Object> temps) {
        if (expr instanceof BinopExpr binopExpr) {
            return execute(binopExpr, temps);
        } else if (expr instanceof CallExpr callExpr) {
            return execute(callExpr, temps);
        } else if (expr instanceof ConstantExpr constantExpr) {
            return execute(constantExpr);
        } else if (expr instanceof EseqExpr eseqExpr) {
            throw new RuntimeException("Cannot execute ESEQ; linearize IRCode!");
        } else if (expr instanceof MemExpr memExpr) {
            return execute(memExpr, temps);
        } else if (expr instanceof NameExpr nameExpr) {
            return execute(nameExpr);
        } else if (expr instanceof TempExpr tempExpr) {
            return execute(tempExpr, temps);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

    private Object execute(BinopExpr binop, Map<Frame.Temp, Object> temps) {
        Integer leftInt = toInt(execute(binop.lhs, temps));
        Integer rightInt = toInt(execute(binop.rhs, temps));

        int result = 0;
        switch (binop.op) {
            case ADD -> result = leftInt + rightInt;
            case SUB -> result = leftInt - rightInt;
            case MUL -> result = leftInt * rightInt;
            case DIV -> result = leftInt / rightInt;
            case MOD -> result = leftInt % rightInt;
            case EQ -> result = (leftInt.equals(rightInt)) ? 1 : 0;
            case NEQ -> result = (!leftInt.equals(rightInt)) ? 1 : 0;
            case GT -> result = (leftInt > rightInt) ? 1 : 0;
            case GEQ -> result = (leftInt >= rightInt) ? 1 : 0;
            case LT -> result = (leftInt < rightInt) ? 1 : 0;
            case LEQ -> result = (leftInt <= rightInt) ? 1 : 0;
            case AND -> result = (leftInt == 1 && rightInt == 1) ? 1 : 0;
            case OR -> result = (leftInt == 0 && rightInt == 0) ? 0 : 1;
        }

        return result;
    }

    private Object execute(CallExpr call, Map<Frame.Temp, Object> temps) {
        if (call.label.name.equals(Constants.printIntLabel)) {
            if (call.args.size() != 2) {
                throw new RuntimeException("Invalid argument count!");
            }
            var arg = execute(call.args.get(1), temps);
            outputStream.ifPresent(stream -> stream.println(arg));
            return null;
        } else if (call.label.name.equals(Constants.printStringLabel)) {
            if (call.args.size() != 2) {
                throw new RuntimeException("Invalid argument count!");
            }
            var address = execute(call.args.get(1), temps);
            var res = memory.ldM(toInt(address));
            outputStream.ifPresent(stream -> stream.println("\"" + res + "\""));
            return null;
        } else if (call.label.name.equals(Constants.printLogLabel)) {
            if (call.args.size() != 2) {
                throw new RuntimeException("Invalid argument count!");
            }
            var arg = execute(call.args.get(1), temps);
            outputStream.ifPresent(stream -> stream.println(toBool(arg)));
            return null;
        } else if (call.label.name.equals(Constants.randIntLabel)) {
            if (call.args.size() != 3) {
                throw new RuntimeException("Invalid argument count!");
            }
            var min = toInt(execute(call.args.get(1), temps));
            var max = toInt(execute(call.args.get(2), temps));
            return random.nextInt(min, max);
        } else if (call.label.name.equals(Constants.seedLabel)) {
            if (call.args.size() != 2) {
                throw new RuntimeException("Invalid argument count!");
            }
            var seed = toInt(execute(call.args.get(1), temps));
            random = new Random(seed);
            return null;
        } else if (memory.ldM(call.label) instanceof CodeChunk chunk) {
            // Pripravimo argumente za naslednjo funkcijo (od SP navzgor)
            int sp = stackPointer;
            for (IRExpr argument : call.args) {
                memory.stM(sp, temps.get(((TempExpr) argument).temp));
                sp += Constants.WordSize;
            }

            internalInterpret(chunk, new HashMap<>());  // Izvedemo naslednjo funkcijo
            return memory.ldM(stackPointer);            // Vrnemo rezultat izvedene funkcije
        } else {
            throw new RuntimeException("Only functions can be called!");
        }
    }

    private Object execute(ConstantExpr constant) {
        return constant.constant;
    }

    private Object execute(MemExpr mem, Map<Frame.Temp, Object> temps) {
        int address = toInt(execute(mem.expr, temps));
        return memory.ldM(address);
    }

    private Object execute(NameExpr name) {
        if (name.label.name.equals(Constants.framePointer))
            return framePointer;
        else if (name.label.name.equals(Constants.stackPointer))
            return stackPointer;
        else
            return memory.address(name.label);
    }

    private Object execute(TempExpr temp, Map<Frame.Temp, Object> temps) {
        return temps.get(temp.temp);
    }

    // ----------- pomožne funkcije -----------

    private int toInt(Object obj) {
        if (obj instanceof Integer integer) {
            return integer;
        }
        throw new IllegalArgumentException("Could not convert obj to integer!");
    }

    private boolean toBool(Object obj) {
        return toInt(obj) == 0 ? false : true;
    }

    private int toInt(boolean bool) {
        return bool ? 1 : 0;
    }

    private String prettyDescription(IRNode ir, int indent) {
        var os = new ByteArrayOutputStream();
        var ps = new PrintStream(os);
        new IRPrettyPrint(ps, indent).print(ir);
        return os.toString(Charset.defaultCharset());
    }

    private String prettyDescription(IRNode ir) {
        return prettyDescription(ir, 2);
    }

    private void prettyPrint(IRNode ir, int indent) {
        System.out.println(prettyDescription(ir, indent));
    }

    private void prettyPrint(IRNode ir) {
        System.out.println(prettyDescription(ir));
    }
}
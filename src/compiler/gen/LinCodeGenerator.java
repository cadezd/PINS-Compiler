/**
 * @ Author: turk
 * @ Description: Linearizacija vmesne kode.
 */

package compiler.gen;

import static common.RequireNonNull.requireNonNull;
import static java.util.List.of;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import common.Constants;
import common.Report;
import compiler.frm.Frame;
import compiler.ir.chunk.Chunk;
import compiler.ir.code.expr.*;
import compiler.ir.code.stmt.*;

public class LinCodeGenerator {
    /**
     * Pomnilnik navideznega stroja. 
     * 
     * Vanj shranimo kodo, globalne spremenljivke ter globalne vrednosti.
     */
    private Memory memory;

    /**
     * Odmik v pomnilniku.
     */
    private int offset = Constants.WordSize; 

    public LinCodeGenerator(Memory memory) {
        requireNonNull(memory);
        this.memory = memory;
    }

    /**
     * 1. Izvedemo linearizacijo kode.
     * 2. V pomnilnik shranimo fragmente programa.
     * 3. Vrnemo kodo funkcije `main`, če le-ta obstaja.
     */
    public Optional<Chunk.CodeChunk> generateCode(List<Chunk> chunks) {
        Optional<Chunk.CodeChunk> mainCodeChunk = Optional.empty();
        for (var chunk : chunks) {
            if (chunk instanceof Chunk.CodeChunk code) {
                var linearChunk = linearizeChunk(code);
                memory.registerLabel(code.frame.label, offset);
                offset += Constants.WordSize;
                memory.stM(code.frame.label, linearChunk);

                if (code.frame.label.name.equals("main")) {
                    if (mainCodeChunk.isEmpty()) {
                        mainCodeChunk = Optional.of(linearChunk);
                    } else {
                        Report.error("Duplicate 'main'");
                    }
                }
            } else if (chunk instanceof Chunk.DataChunk data) {
                memory.registerLabel(data.access.label, offset);
                memory.stM(offset, data.data);
                offset += data.access.size;
            } else if (chunk instanceof Chunk.GlobalChunk global) {
                memory.registerLabel(global.access.label, offset);
                offset += global.access.size;
            }
        }
        return mainCodeChunk;
    }
    
    private Chunk.CodeChunk linearizeChunk(Chunk.CodeChunk chunk) {
        var linCode = linearize(chunk.code);
        return new Chunk.CodeChunk(chunk.frame, linCode);
    }

    private EseqExpr linearize(IRExpr expr) {
        if (expr instanceof BinopExpr binopExpr) {
            return linearize(binopExpr);
        } else if (expr instanceof CallExpr callExpr) {
            return linearize(callExpr);
        } else if (expr instanceof ConstantExpr constantExpr) {
            return linearize(constantExpr);
        } else if (expr instanceof EseqExpr eseqExpr) {
            return linearize(eseqExpr);
        } else if (expr instanceof MemExpr memExpr) {
            return linearize(memExpr);
        } else if (expr instanceof NameExpr nameExpr) {
            return linearize(nameExpr);
        } else if (expr instanceof TempExpr tempExpr) {
            return linearize(tempExpr);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

    private SeqStmt linearize(IRStmt stmt) {
        if (stmt instanceof CJumpStmt cJumpStmt) {
            return linearize(cJumpStmt);
        } else if (stmt instanceof ExpStmt expStmt) {
            return linearize(expStmt);
        } else if (stmt instanceof JumpStmt jumpStmt) {
            return linearize(jumpStmt);
        } else if (stmt instanceof LabelStmt labelStmt) {
            return linearize(labelStmt);
        } else if (stmt instanceof MoveStmt moveStmt) {
            return linearize(moveStmt);
        } else if (stmt instanceof SeqStmt seqStmt) {
            return linearize(seqStmt);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

    private EseqExpr linearize(BinopExpr binop) {
        var lhs = linearize(binop.lhs);
        var rhs = linearize(binop.rhs);
        return new EseqExpr(
            new SeqStmt(flatten(of(
                lhs.stmt.statements(),
                rhs.stmt.statements()
            ))), 
            new BinopExpr(lhs.expr, rhs.expr, binop.op));
    }

    private EseqExpr linearize(CallExpr call) {
        var allStatements = new SeqStmt(new ArrayList<>());
        var args = new ArrayList<IRExpr>(call.args.size());
        for (var arg : call.args) {
            var eseq = linearize(arg);
            allStatements.statements.addAll(eseq.stmt.statements());
            var temp = new TempExpr(Frame.Temp.next());
            var move = new MoveStmt(
                temp,
                eseq.expr);
            allStatements.statements.add(move);
            args.add(temp);
        }
        return new EseqExpr(
            allStatements, 
            new CallExpr(call.label, args));
    }

    private EseqExpr linearize(ConstantExpr constant) {
        return new EseqExpr(SeqStmt.empty(), constant);
    }

    private EseqExpr linearize(EseqExpr eseq) {
        var linStmt = linearize(eseq.stmt).statements;
        var linExpr = linearize(eseq.expr);
        return new EseqExpr(
            new SeqStmt(flatten(of(
                linStmt,
                linExpr.stmt.statements()
            ))), 
            linExpr.expr);
    }

    private EseqExpr linearize(MemExpr mem) {
        var linExpr = linearize(mem.expr);
        return new EseqExpr(
            new SeqStmt(linExpr.stmt.statements()), 
            new MemExpr(linExpr.expr));
    }

    private EseqExpr linearize(NameExpr name) {
        return new EseqExpr(SeqStmt.empty(), name);
    }

    private EseqExpr linearize(TempExpr temp) {
        return new EseqExpr(SeqStmt.empty(), temp);
    }

    private SeqStmt linearize(CJumpStmt cjump) {
        var linCond = linearize(cjump.condition);
        return new SeqStmt(
            flatten(of(
                linCond.stmt.statements(),
                new CJumpStmt(linCond.expr, cjump.thenLabel, cjump.elseLabel).statements()
            )));
    }

    private SeqStmt linearize(ExpStmt exp) {
        var linExpr = linearize(exp.expr);
        return new SeqStmt(flatten(of(
            linExpr.stmt.statements(),
            new ExpStmt(linExpr.expr).statements()
        )));
    }

    private SeqStmt linearize(JumpStmt jump) {
        return new SeqStmt(of(jump));
    }

    private SeqStmt linearize(LabelStmt label) {
        return new SeqStmt(of(label));
    }

    private SeqStmt linearize(MoveStmt move) {
        var linDst = linearize(move.dst);
        var linSrc = linearize(move.src);
        return new SeqStmt(flatten(of(
            linDst.stmt.statements(),
            linSrc.stmt.statements(),
            new MoveStmt(linDst.expr, linSrc.expr).statements()
        )));
    }

    private SeqStmt linearize(SeqStmt seq) {
        var linStmts = seq.statements.stream()
            .map(stmt -> linearize(stmt).statements())
            .collect(Collectors.toList());
        return new SeqStmt(flatten(linStmts));
    }

    private <T> List<T> flatten(List<List<T>> lists) {
        var size = lists.stream().mapToInt(list -> list.size()).sum();
        List<T> res = new ArrayList<>(size);
        for (var list : lists) {
            res.addAll(list);
        }
        return res;
    } 
}

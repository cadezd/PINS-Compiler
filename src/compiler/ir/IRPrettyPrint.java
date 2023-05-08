/**
 * @ Author: turk
 * @ Description: Izpis drevesa vmesne kode.
 */

package compiler.ir;

import static common.StringUtil.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.List;

import common.VoidOperator;
import compiler.ir.chunk.Chunk;
import compiler.ir.code.IRNode;
import compiler.ir.code.expr.*;
import compiler.ir.code.stmt.*;

public class IRPrettyPrint {
    /**
     * Izhodni tok, kamor se izpiše drevo vmesne kode.
     */
    private final PrintStream outputStream;

    /**
     * Za koliko naj se indentacija poveča pri gnezdenju.
     */
    private final int increaseIndentBy;
    
    /**
     * Trenutna indentacija.
     */
    private int indent = 0;

    public IRPrettyPrint(PrintStream outputStream, int increaseIndentBy) {
        requireNonNull(outputStream);
        this.outputStream = outputStream;
        this.increaseIndentBy = increaseIndentBy;
    }

    public IRPrettyPrint(PrintStream outputStream) {
        requireNonNull(outputStream);
        this.outputStream = outputStream;
        this.increaseIndentBy = 4;
    }

    // --------------------------------------

    public void print(List<Chunk> chunks) {
        chunks.forEach(chunk -> print(chunk));
    }

    public void print(Chunk chunk) {
        if (chunk instanceof Chunk.CodeChunk code) {
            print(code);
        } else if (chunk instanceof Chunk.DataChunk data) {
            print(data);
        } else if (chunk instanceof Chunk.GlobalChunk global) {
            print(global);
        }
    }

    public void print(Chunk.CodeChunk chunk) {
        println(chunk.frame.toString());
        print(chunk.code);
    }

    public void print(Chunk.DataChunk chunk) {
        println(chunk.access.toString(), ": ", chunk.data);
    }

    public void print(Chunk.GlobalChunk chunk) {
        println(chunk.access.toString(), ": ");
    }

    public void print(IRNode node) {
        if (node instanceof IRStmt stmt) {
            print(stmt);
        } else if (node instanceof IRExpr expr) {
            print(expr);
        } else {
            throw new IllegalArgumentException("Unknown node type");
        }
    }

    public void print(IRExpr expr) {
        if (expr instanceof BinopExpr binopExpr) {
            print(binopExpr);
        } else if (expr instanceof CallExpr callExpr) {
            print(callExpr);
        } else if (expr instanceof ConstantExpr constantExpr) {
            print(constantExpr);
        } else if (expr instanceof EseqExpr eseqExpr) {
            print(eseqExpr);
        } else if (expr instanceof MemExpr memExpr) {
            print(memExpr);
        } else if (expr instanceof NameExpr nameExpr) {
            print(nameExpr);
        } else if (expr instanceof TempExpr tempExpr) {
            print(tempExpr);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

    public void print(IRStmt stmt) {
        if (stmt instanceof CJumpStmt cJumpStmt) {
            print(cJumpStmt);
        } else if (stmt instanceof ExpStmt expStmt) {
            print(expStmt);
        } else if (stmt instanceof JumpStmt jumpStmt) {
            print(jumpStmt);
        } else if (stmt instanceof LabelStmt labelStmt) {
            print(labelStmt);
        } else if (stmt instanceof MoveStmt moveStmt) {
            print(moveStmt);
        } else if (stmt instanceof SeqStmt seqStmt) {
            print(seqStmt);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

    public void print(BinopExpr binop) {
        println("BINOP ", binop.op.toString(), ":");
        inNewScope(() -> {
            print(binop.lhs);
            print(binop.rhs);
        });
    }

    public void print(CallExpr call) {
        println("CALL ", call.label.toString(), ":");
        inNewScope(() -> {
            call.args.stream().forEach(arg -> print(arg));
        });
    }

    public void print(ConstantExpr constant) {
        println("CONSTANT: ", String.valueOf(constant.constant));
    }

    public void print(EseqExpr eseq) {
        println("ESEQ:");
        inNewScope(() -> {
            print(eseq.stmt);
            print(eseq.expr);
        });
    }

    public void print(MemExpr mem) {
        println("MEM:");
        inNewScope(() -> print(mem.expr));
    }

    public void print(NameExpr name) {
        println("NAME: ", name.label.toString());
    }

    public void print(TempExpr temp) {
        println("TEMP: ", temp.temp.toString());
    }

    public void print(CJumpStmt cjump) {
        println("CJUMP:");
        inNewScope(() -> {
            print(cjump.condition);
            println(cjump.thenLabel.toString());
            println(cjump.elseLabel.toString());
        });
    }

    public void print(ExpStmt exp) {
        println("EXP:");
        inNewScope(() -> print(exp.expr));
    }

    public void print(JumpStmt jump) {
        println("JUMP:");
        inNewScope(() -> println(jump.label.toString()));
    }

    public void print(LabelStmt label) {
        println("LABEL: ", label.label.toString());
    }

    public void print(MoveStmt move) {
        println("MOVE:");
        inNewScope(() -> {
            print(move.dst);
            print(move.src);
        });
    }

    public void print(SeqStmt seq) {
        println("SEQ:");
        inNewScope(() -> {
            seq.statements.stream().forEach(stmt -> print(stmt));
        });
    }

    // --------------------------------------

    private void println(String... args) {
        outputStream.print(indented("", indent));
        for (var arg : args) {
            outputStream.print(arg);
        }
        outputStream.println();
    }

    private void inNewScope(VoidOperator op) {
        indent += increaseIndentBy;
        op.apply();
        indent -= increaseIndentBy;
    }
}

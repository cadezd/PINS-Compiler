/**
 * @ Author: turk
 * @ Description: Binarni izraz.
 */

package compiler.ir.code.expr;

import static common.RequireNonNull.requireNonNull;

public class BinopExpr extends IRExpr {
    /**
     * Levi operand.
     */
    public final IRExpr lhs;

    /**
     * Desni operand.
     */
    public final IRExpr rhs;

    /**
     * Operator.
     */
    public final Operator op;

    public BinopExpr(IRExpr lhs, IRExpr rhs, Operator op) {
        requireNonNull(lhs, rhs, op);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    public static enum Operator {
        ADD, SUB, MUL, DIV, MOD, // aritmetični
        AND, OR, // logični
        EQ, NEQ, LT, GT, LEQ, GEQ // primerjalni
    }
}

/**
 * @ Author: turk
 * @ Description: Prenos vrednosti.
 */

package compiler.ir.code.stmt;

import static common.RequireNonNull.requireNonNull;

import compiler.ir.code.expr.IRExpr;

public class MoveStmt extends IRStmt {
    /**
     * Ponor.
     */
    public final IRExpr dst;

    /**
     * Izvor.
     */
    public final IRExpr src;

    public MoveStmt(IRExpr dst, IRExpr src) {
        requireNonNull(dst, src);
        this.dst = dst;
        this.src = src;
    }
}

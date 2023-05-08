/**
 * @ Author: turk
 * @ Description: Stavek + izraz.
 */

package compiler.ir.code.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.ir.code.stmt.IRStmt;

public class EseqExpr extends IRExpr {
    /**
     * Stavek.
     */
    public final IRStmt stmt;

    /**
     * Izraz.
     */
    public final IRExpr expr;

    public EseqExpr(IRStmt stmt, IRExpr expr) {
        requireNonNull(stmt, expr);
        this.stmt = stmt;
        this.expr = expr;
    }
}

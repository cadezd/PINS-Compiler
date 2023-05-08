/**
 * @ Author: turk
 * @ Description: Izraz kot stavek.
 */

package compiler.ir.code.stmt;

import static common.RequireNonNull.requireNonNull;

import compiler.ir.code.expr.IRExpr;

public class ExpStmt extends IRStmt {
    /**
     * Izraz.
     */
    public final IRExpr expr;
    
    public ExpStmt(IRExpr expr) {
        requireNonNull(expr);
        this.expr = expr;
    }
}

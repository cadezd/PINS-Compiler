/**
 * @ Author: turk
 * @ Description: Dostop do pomnilnika.
 */

package compiler.ir.code.expr;

import static common.RequireNonNull.requireNonNull;

public class MemExpr extends IRExpr {
    /**
     * Dereferenciran izraz.
     */
    public final IRExpr expr;

    public MemExpr(IRExpr expr) {
        requireNonNull(expr);
        this.expr = expr;
    }
}

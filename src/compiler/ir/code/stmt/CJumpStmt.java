/**
 * @ Author: turk
 * @ Description: Pogojni skok.
 */

package compiler.ir.code.stmt;

import static common.RequireNonNull.requireNonNull;

import compiler.frm.Frame;
import compiler.ir.code.expr.IRExpr;

public class CJumpStmt extends IRStmt {
    /**
     * Pogojni izraz.
     */
    public final IRExpr condition;

    /**
     * Labela skoka, če je pogoj izpolnjen.
     */
    public final Frame.Label thenLabel;

    /**
     * Labela skoka, če pogoj ni izpolnjen.
     */
    public final Frame.Label elseLabel;
    
    public CJumpStmt(IRExpr condition, Frame.Label thenLabel, Frame.Label elseLabel) {
        requireNonNull(condition, thenLabel, elseLabel);
        this.condition = condition;
        this.thenLabel = thenLabel;
        this.elseLabel = elseLabel;
    }
}

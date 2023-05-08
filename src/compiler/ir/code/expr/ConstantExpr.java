/**
 * @ Author: turk
 * @ Description: Konstanta.
 */

package compiler.ir.code.expr;

public class ConstantExpr extends IRExpr {
    /**
     * Vrednost.
     */
    public final int constant;

    public ConstantExpr(int constant) {
        this.constant = constant;
    }
}

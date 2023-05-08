/**
 * @ Author: turk
 * @ Description: Začasna spremenljivka.
 */

package compiler.ir.code.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.frm.Frame;
import compiler.frm.Frame.Temp;

public class TempExpr extends IRExpr {
    /**
     * Začasna spremenljivka.
     */
    public final Frame.Temp temp;

    public TempExpr(Temp temp) {
        requireNonNull(temp);
        this.temp = temp;
    }
}

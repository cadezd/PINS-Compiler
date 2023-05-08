/**
 * @ Author: turk
 * @ Description: Klic funkcije.
 */

package compiler.ir.code.expr;

import java.util.List;
import static common.RequireNonNull.requireNonNull;

import compiler.frm.Frame;
import compiler.frm.Frame.Label;

public class CallExpr extends IRExpr {
    /**
     * Labela funkcije.
     */
    public final Frame.Label label;

    /**
     * Argumenti funkcije.
     */
    public final List<IRExpr> args;

    public CallExpr(Label label, List<IRExpr> args) {
        requireNonNull(label, args);
        this.label = label;
        this.args = args;
    }
}

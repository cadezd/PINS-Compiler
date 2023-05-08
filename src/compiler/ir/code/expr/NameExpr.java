/**
 * @ Author: turk
 * @ Description: Ime.
 */

package compiler.ir.code.expr;

import static common.RequireNonNull.requireNonNull;

import common.Constants;
import compiler.frm.Frame;

public class NameExpr extends IRExpr {
    /**
     * Labela imenovane lokacije.
     */
    public final Frame.Label label;
    
    public NameExpr(Frame.Label label) {
        requireNonNull(label);
        this.label = label;
    }

    /**
     * Kazalec na vrh klicnega zapisa.
     */
    public static NameExpr FP() {
        return new NameExpr(Frame.Label.named(Constants.framePointer));
    }

    /**
     * Kazalec na dno klicnega zapisa.
     */
    public static NameExpr SP() {
        return new NameExpr(Frame.Label.named(Constants.stackPointer));
    }
}

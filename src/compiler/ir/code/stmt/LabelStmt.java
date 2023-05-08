/**
 * @ Author: turk
 * @ Description: Labela.
 */

package compiler.ir.code.stmt;

import static common.RequireNonNull.requireNonNull;

import compiler.frm.Frame;

public class LabelStmt extends IRStmt {
    /**
     * Labela imenovane lokacije.
     */
    public final Frame.Label label;

    public LabelStmt(Frame.Label label) {
        requireNonNull(label);
        this.label = label;
    }
}

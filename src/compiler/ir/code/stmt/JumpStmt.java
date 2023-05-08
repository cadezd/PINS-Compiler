/**
 * @ Author: turk
 * @ Description: Brezpogojni skok.
 */

package compiler.ir.code.stmt;

import static common.RequireNonNull.requireNonNull;

import compiler.frm.Frame;

public class JumpStmt extends IRStmt {
    /**
     * Labela skoka.
     */
    public final Frame.Label label;

    public JumpStmt(Frame.Label label) {
        requireNonNull(label);
        this.label = label;
    }
}

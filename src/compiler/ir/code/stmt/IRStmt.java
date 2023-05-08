/**
 * @ Author: turk
 * @ Description: Stavek.
 */

package compiler.ir.code.stmt;

import java.util.List;

import compiler.ir.code.IRNode;

public abstract class IRStmt extends IRNode {
    /**
     * Pretvori stavek v seznam stavkov.
     * 
     * V primeru, da je ta objekt instanca `SeqStmt`,
     * je rezultat seznam stavkov stavka `SeqStmt`.
     * Sicer je rezultat seznam, v katerem se nahaja ta objekt.
     */
    public List<IRStmt> statements() {
        if (this instanceof SeqStmt seq) {
            return seq.statements;
        }
        return List.of(this);
    }
}

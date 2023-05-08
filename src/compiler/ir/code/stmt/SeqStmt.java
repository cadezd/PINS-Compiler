/**
 * @ Author: turk
 * @ Description: Zaporedje stavkov.
 */

package compiler.ir.code.stmt;

import java.util.List;
import static common.RequireNonNull.requireNonNull;

public class SeqStmt extends IRStmt {
    /**
     * Stavki.
     */
    public final List<IRStmt> statements;

    public SeqStmt(List<IRStmt> statements) {
        requireNonNull(statements);
        this.statements = statements;
    }
    
    public static SeqStmt empty() {
        return new SeqStmt(List.of());
    }
}

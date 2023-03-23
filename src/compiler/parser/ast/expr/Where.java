/**
 * @Author: turk
 * @Description: Lokalne definicije.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;
import compiler.parser.ast.def.Defs;

public class Where extends Expr {
    /**
     * Izraz, ki se izvede v gnezdenem območju.
     */
    public final Expr expr;

    /**
     * Definicije.
     */
    public final Defs defs;

    public Where(Position position, Expr expr, Defs defs) {
        super(position);
        requireNonNull(expr);
        requireNonNull(defs);
        this.expr = expr;
        this.defs = defs;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

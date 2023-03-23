/**
 * @Author: turk
 * @Description: While zanka.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class While extends Expr {
    /**
     * Pogojni izraz.
     */
    public final Expr condition;

    /**
     * Jedro zanke.
     */
    public final Expr body;

    public While(Position position, Expr condition, Expr body) {
        super(position);
        requireNonNull(condition);
        requireNonNull(body);
        this.condition = condition;
        this.body = body;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

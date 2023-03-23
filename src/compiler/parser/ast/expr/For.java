/**
 * @Author: turk
 * @Description: For zanka.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class For extends Expr {
    /**
     * Števec.
     */
    public final Name counter;

    /**
     * Spodnja meja.
     */
    public final Expr low;

    /**
     * Zgornja meja.
     */
    public final Expr high;

    /**
     * Korak.
     */
    public final Expr step;

    /**
     * Jedro zanke.
     */
    public final Expr body;    

    public For(Position position, Name counter, Expr low, Expr high, Expr step, Expr body) {
        super(position);
        requireNonNull(counter);
        requireNonNull(low);
        requireNonNull(high);
        requireNonNull(step);
        requireNonNull(body);
        this.counter = counter;
        this.low = low;
        this.high = high;
        this.step = step;
        this.body = body;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

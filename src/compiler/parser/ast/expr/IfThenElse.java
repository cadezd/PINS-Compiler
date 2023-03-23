/**
 * @Author: turk
 * @Description: Pogojni izraz. 
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

import java.util.Optional;

public class IfThenElse extends Expr {
    /**
     * Pogojni izraz.
     */
    public final Expr condition;

    /**
     * Pozitivna veja.
     */
    public final Expr thenExpression;

    /**
     * Negativna veja. V primeru, da gre za `If-Then` izraz,
     * je vrednost enaka `Optional.empty()`.
     */
    public final Optional<Expr> elseExpression;

    /**
     * Ustvari nov `If-Then` izraz.
     */
    public IfThenElse(Position position, Expr condition, Expr thenExpression) {
        super(position);
        requireNonNull(condition);
        requireNonNull(thenExpression);
        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = Optional.empty();
    }

    /**
     * Ustvari nov `If-Then-Else` izraz.
     */
    public IfThenElse(Position position, Expr condition, Expr thenExpression, Expr elseExpression) {
        super(position);
        requireNonNull(condition);
        requireNonNull(thenExpression);
        requireNonNull(elseExpression);
        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = Optional.of(elseExpression);
    }

    /**
     * Ustvari nov `If-Then-Else` izraz.
     * 
     * Če je vrednost parametra `elseExpression` enaka `Optional.empty`()`,
     * potem gre za `If-Then` izraz, sicer za `If-Then-Else` izraz.
     */
    public IfThenElse(Position position, Expr condition, Expr thenExpression, Optional<Expr> elseExpression) {
        super(position);
        requireNonNull(condition);
        requireNonNull(thenExpression);
        requireNonNull(elseExpression);
        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = elseExpression;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

/**
 * @Author: turk
 * @Description: Funkcijski klic.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

import java.util.List;

public class Call extends Expr {
    /**
     * Argumenti.
     */
    public final List<Expr> arguments;

    /**
     * Ime funkcije, ki jo kličemo.
     */
    public final String name;

    public Call(Position position, List<Expr> arguments, String name) {
        super(position);
        requireNonNull(arguments);
        requireNonNull(name);
        this.arguments = arguments;
        this.name = name;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

/**
 * @Author: turk
 * @Description: Ime spremenljivke.
 */

package compiler.parser.ast.expr;

import static compiler.common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class Name extends Expr {
    /**
     * Ime spremenljivke.
     */
    public final String name;

    public Name(Position position, String name) {
        super(position);
        requireNonNull(name);
        this.name = name;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

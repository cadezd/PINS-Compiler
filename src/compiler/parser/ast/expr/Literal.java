/**
 * @Author: turk
 * @Description: Konstanta.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;
import compiler.parser.ast.type.Atom;

public class Literal extends Expr {
    /**
     * Vrednost konstante.
     */
    public final String value;

    /**
     * Tip konstante.
     */
    public final Atom.Type type;

    public Literal(Position position, String value, Atom.Type type) {
        super(position);
        requireNonNull(value);
        requireNonNull(type);
        this.value = value;
        this.type = type;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

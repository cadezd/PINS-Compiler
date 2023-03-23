/**
 * @Author: turk
 * @Description: Blok izrazov.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

import java.util.List;

public class Block extends Expr {
    /**
     * Izrazi.
     */
    public final List<Expr> expressions;

    public Block(Position position, List<Expr> expressions) {
        super(position);
        requireNonNull(expressions);
        this.expressions = expressions;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

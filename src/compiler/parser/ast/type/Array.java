/**
 * @Author: turk
 * @Description: Podatkovni tip, ki predstavlja tabelo.
 */

package compiler.parser.ast.type;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class Array extends Type {
    /**
     * Velikost tabele.
     */
    public final int size;

    /**
     * Tip elementov.
     */
    public final Type type;

    public Array(Position position, int size, Type type) {
        super(position);
        requireNonNull(size);
        requireNonNull(type);
        this.size = size;
        this.type = type;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

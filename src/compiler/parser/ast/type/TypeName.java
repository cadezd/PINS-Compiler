/**
 * @Author: turk
 * @Description: Ime tipa.
 */

package compiler.parser.ast.type;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class TypeName extends Type {
    /**
     * Ime tipa.
     */
    public final String identifier;

    public TypeName(Position position, String identifier) {
        super(position);
        requireNonNull(identifier);
        this.identifier = identifier;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }
}

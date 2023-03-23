/*
 * @Author: turk
 * @Description: Atomarni podatkovni tip.
 */

package compiler.parser.ast.type;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class Atom extends Type {
    /**
     * Vrsta tipa.
     */
    public final Type type;

    private Atom(Position position, Type type) {
        super(position);
        requireNonNull(type);
        this.type = type;
    }

    /**
     * Vrne nov atomarni tip integer.
     */
    public static Atom INT(Position position) {
        return new Atom(position, Type.INT);
    }

    /**
     * Vrne nov atomarni tip logical.
     */
    public static Atom LOG(Position position) {
        return new Atom(position, Type.LOG);
    }

    /**
     * Vrne nov atomarni tip string.
     */
    public static Atom STR(Position position) {
        return new Atom(position, Type.STR);
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

    public static enum Type {
        INT, LOG, STR
    }
}

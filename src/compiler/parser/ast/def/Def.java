/**
 * @Author: turk
 * @Description: Vozlišče v abstraktnem sintaksnem drevesu,
 * ki predstavlja definicijo.
 */

package compiler.parser.ast.def;

import static common.RequireNonNull.requireNonNull;

import compiler.lexer.Position;
import compiler.parser.ast.Ast;

public abstract class Def extends Ast {
    /**
     * Ime definicije.
     */
    public final String name;

    public Def(Position position, String name) {
        super(position);
        requireNonNull(name);
        this.name = name;
    }
}

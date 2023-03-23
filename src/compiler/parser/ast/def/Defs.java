/**
 * @ Author: turk
 * @ Description: Seznam definicij.
 */

package compiler.parser.ast.def;

import static common.RequireNonNull.requireNonNull;

import java.util.List;

import compiler.common.Visitor;
import compiler.lexer.Position;
import compiler.parser.ast.Ast;

public class Defs extends Ast {
    /**
     * Definicije.
     */
    public final List<Def> definitions;

    public Defs(Position position, List<Def> definitions) {
        super(position);
        requireNonNull(definitions);
        this.definitions = definitions;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}

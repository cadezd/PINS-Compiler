/**
 * @Author: turk
 * @Description: Vozlišče v abstraktnem sintaksnem drevesu,
 * ki predstavlja izraz.
 */

package compiler.parser.ast.expr;

import compiler.lexer.Position;
import compiler.parser.ast.Ast;

public abstract class Expr extends Ast {
    public Expr(Position position) {
        super(position);
    }
}

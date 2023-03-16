/**
 * @Author: turk
 * @Description: Vozlišče v abstraktnem sintaksnem drevesu, 
 * ki predstavlja podatkovni tip.
 */

package compiler.parser.ast.type;

import compiler.lexer.Position;
import compiler.parser.ast.Ast;

public abstract class Type extends Ast {
    public Type(Position position) {
        super(position);
    }
}

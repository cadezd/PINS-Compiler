/**
 * @ Author: turk
 * @ Description: Vozlišče v abstraktnem sintaksnem drevesu.
 */

package compiler.parser.ast;

import compiler.common.Visitor;
import compiler.lexer.Position;

public abstract class Ast {
    /**
     * Lokacija vozlišča v izvorni kodi.
     */
	public final Position position;

	/**
	 * Ustvari novo vozlišče.
	 * 
	 * @param pos Lokacija.
	 */
	public Ast(Position position) {
		this.position = position;
	}

    /**
     * 'Sprejmi' obiskovalca.
     */
	public abstract void accept(Visitor visitor);
}

/**
 * @ Author: turk
 * @ Description: Leksikalni simbol.
 */

package compiler.lexer;

import static common.RequireNonNull.requireNonNull;

public class Symbol {
    /**
     * Območje simbola znotraj izvorne datoteke.
     */
    public final Position position;

    /**
     * Vrsta simbola.
     */
    public final TokenType tokenType;

    /**
     * Znakovna predstavitev simbola.
     */
    public final String lexeme;

    /**
     * Ustvari nov leksikalni simbol.
     * 
     * @param position Območje, ki ga simbol zajema v izvorni datoteki.
     * @param tokenType Vrsta simbola.
     * @param lexeme Znakovna predstavitev simbola.
     */
    public Symbol(Position position, TokenType tokenType, String lexeme) {
        requireNonNull(position, tokenType, lexeme);
        this.position = position;
        this.tokenType = tokenType;
        this.lexeme = lexeme;
    }

    /**
     * Ustvari nov leksikalni simbol.
     * 
     * @param startLocation Začetna lokacija območja, ki ga simbol zajema v izvorni datoteki.
     * @param endLocation Končna lokacija območja, ki ga simbol zajema v izvorni datoteki.
     * @param tokenType Vrsta simbola.
     * @param lexeme Znakovna predstavitev simbola.
     */
    public Symbol(Position.Location startLocation, Position.Location endLocation, TokenType tokenType, String lexeme) {
        this(new Position(startLocation, endLocation), tokenType, lexeme);
    }

    @Override
    public String toString() {
        if (tokenType == TokenType.EOF) {
            return tokenType + ":" + lexeme;
        }
        return position.toString()+" "+tokenType + ":" + lexeme;
    }

    // @todo:
    // override equals() and hashCode() ?
}

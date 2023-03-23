/**
 * @ Author: turk
 * @ Description: Vmesnik, ki predstavlja simbolno tabelo.
 */

package compiler.seman.name.env;

import java.util.Optional;

import common.VoidOperator;
import compiler.parser.ast.def.Def;

public interface SymbolTable {
    /**
     * Vstavi novo definicijo v simbolno tabelo.
     * 
     * V primeru, da definicija na trenutnem nivoju
     * gnezdenja že obstaja, vrne ``DefinitionAlreadyExistsException``.
     * 
     * @param definition definicija
     */
    void insert(Def definition) throws DefinitionAlreadyExistsException;

    /**
     * Vrni definicijo za ime ali `Optional.empty()`,
     * če definicija s podanim imenom ne obstaja.
     * 
     * @param name ime definicije
     */
    Optional<Def> definitionFor(String name);

    /**
     * Povečaj nivo gnezdenja.
     */
    void pushScope();

    /**
     * Zmanjšaj nivo gnezednja.
     */
    void popScope();

    /**
     * Izvedi operacijo znotraj novega nivoja gnezdenja.
     */
    default void inNewScope(VoidOperator op) {
        pushScope();
        op.apply();
        popScope();
    }

    /**
     * Napaka v primeru vstavljanja že obstoječe definicije.
     */
    public static class DefinitionAlreadyExistsException extends Exception {
        public final Def definition;

        public DefinitionAlreadyExistsException(Def definition) {
            this.definition = definition;
        }
    }
}

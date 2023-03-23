/**
 * @ Author: turk
 * @ Description: Določa območje znotraj izvorne datoteke.
 */

package compiler.lexer;

import static common.RequireNonNull.requireNonNull;

public class Position {
    /**
     * Začetna lokacija območja.
     */
    public final Location start;

    /**
     * Končna lokacija območja.
     */
    public final Location end;

    /**
     * Ustvari novo območje.
     * 
     * @param start Začetna lokacija.
     * @param end Končna lokacija.
     */
    public Position(Location start, Location end) {
        requireNonNull(start, end);
        this.start = start;
        this.end = end;
    }

    /**
     * Ustvari novo območje.
     * 
     * @param startLine Vrstica začetne lokacije.
     * @param startCol Stolpec začetne lokacije.
     * @param endLine Vrstica končne lokacije.
     * @param endCol Stolpec končne lokacije.
     */
    public Position(int startLine, int startCol, int endLine, int endCol) {
        this(new Location(startLine, startCol), new Location(endLine, endCol));
    }

    /**
     * Ustvari novo _ničelno_ območje.
     */
    public static Position zero() {
        return new Position(Location.zero(), Location.zero());
    }

    /**
     * Ustvari novo območje, čigar začetna in končna lokacija sta enaki.
     */
    public static Position fromLocation(Location location) {
        return new Position(location, location);
    }

    @Override
    public String toString() {
        if (start.toString().equals(end.toString())) {
            return "[" +  start.toString() + "]";
        }
        return "["+start.toString() + "-" + end.toString()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Position other) {
            return this.start.equals(other.start) && this.end.equals(other.end);
        }
        return false;
    }

    @Override
    public int hashCode() {
        var result = 17;
        result = 31 * result + start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    /**
     * Razred, ki hrani lokacijo (vrstica, stolpec) znotraj izvorne datoteke.
     */
    public static class Location {
        /**
         * Vrstica.
         */
        public final int line;

        /**
         * Stolpec.
         */
        public final int column;

        /**
         * Ustvari novo lokacijo.
         * 
         * @param line Vrstica.
         * @param column Stolpec.
         */
        public Location(int line, int column) {
            this.line = line;
            this.column = column;
        }

        /**
         * Ustvari novo lokacijo, ki kaže na začetek datoteke.
         */
        public static Location zero() {
            return new Location(0, 0);
        }

        // ----------------------------

        @Override
        public String toString() {
            return line + ":" + column;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Location other) &&
                this.line == other.line && this.column == other.column;
        }

        @Override
        public int hashCode() {
            var result = 17;
            result = 31 * result + line;
            result = 31 * result + column;
            return result;
        }
    }
}

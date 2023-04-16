/**
 * @ Author: turk
 * @ Description: Opis dostopa do spremenljivke.
 */

package compiler.frm;

import static common.RequireNonNull.requireNonNull;

import compiler.frm.Frame.Label;

public abstract class Access {
    /**
    * Velikost spremenljivke.
    */
    public final int size;

    public Access(int size) {
        this.size = size;
    }

    /**
     * Opis dostopa do vrednosti, ki se nahaja na skladu.
     */
    public static abstract class Stack extends Access {
        /**
         * Odmik lokalne spremenljivke od vrha klicnega zapisa.
         */
        public final int offset;

        /**
         * Statični nivo, na katerem je vrednost definirana.
         */
        public final int staticLevel;

        public Stack(int size, int offset, int staticLevel) {
            super(size);
            this.offset = offset;
            this.staticLevel = staticLevel;
        }
    }

    /**
     * Opis dostopa do lokalne spremenljivke.
     */
    public static class Local extends Stack {
        public Local(int size, int offset, int staticLevel) {
            super(size, offset, staticLevel);
        }

        @Override
        public String toString() {
            return "Local: size["+size+"],offset["+offset+"],sl["+staticLevel+"]";
        }
    }

    /**
     * Opis dostopa do parametra funkcije.
     */
    public static class Parameter extends Stack {
        public Parameter(int size, int offset, int staticLevel) {
            super(size, offset, staticLevel);
        }

        @Override
        public String toString() {
            return "Parameter: size["+size+"],offset["+offset+"],sl["+staticLevel+"]";
        }
    }

    /**
     * Globalni dostop (preko labele).
     */
    public static class Global extends Access {
        public final Label label;

        public Global(int size, Label label) {
            super(size);
            requireNonNull(label);
            this.label = label;
        }

        @Override
        public String toString() {
            return "Global: size["+size+"],label["+label.toString()+"]";
        }
    }
}

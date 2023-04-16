/**
 * @ Author: turk
 * @ Description: Klicni zapis.
 */

package compiler.frm;

import common.Constants;
import static common.RequireNonNull.requireNonNull;

public class Frame {
    /**
     * Vstopna labela funkcije.
     */
    public final Label label;

    /**
     * Statični nivo, na katerem je funkcija definirana.
     */
    public final int staticLevel;

    /**
     * Velikost parametrov.
     */
    public final int parametersSize;

    /**
     * Velikost argumentov.
     */
    public final int argumentsSize;

    /**
     * Velikost lokalnih spremenljivk.
     */
    public final int localsSize;

    public Frame(
        Label label, 
        int staticLevel, 
        int parametersSize, 
        int argumentsSize, 
        int localsSize
    ) {
        requireNonNull(label);
        this.label = label;
        this.staticLevel = staticLevel;
        this.parametersSize = parametersSize;
        this.argumentsSize = argumentsSize;
        this.localsSize = localsSize;
    }

    /**
     * @return velikost klicnega zapisa
     */
    public int size() {
        return localsSize + Constants.WordSize + Math.max(Constants.WordSize, argumentsSize);
        //          (oldFP) ~~~~~~~~~~~~~~~~~~            ~~~~~~~~~~~~~~~~~~ (RV)
    }

    public int oldFPOffset() {
        return localsSize + Constants.WordSize;
    }

    @Override
    public String toString() {
        return "FRAME [" + label.toString() + "]: " +
                "level=" + staticLevel + "," +
                "locals_size=" + localsSize + "," +
                "arguments_size=" + argumentsSize + "," +
                "parameters_size=" + parametersSize + "," +
                "size=" + size();
    }

    /**
     * Začasna spremenljivka.
     */
    public static class Temp {
        private static int count = 0;

        /**
         * 'Ime' začasne spremenljivke.
         */
        public final int id;

        private Temp(int id) {
            this.id = id;
        }

        /**
         * Ustvari novo začasno spremenljivko.
         */
        public static Temp next() {
            return new Temp(count++);
        }

        @Override
        public String toString() {
            return "T[" + id + "]";
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Temp t) && t.id == this.id;
        }
    
        @Override
        public int hashCode() {
            var result = 17;
            result = 31 * result + id;
            return result;
        }
    }

    /**
     * Labela ('strojni naslov') v programu.
     */
    public static class Label {
        /**
         * Števec anonimnih label.
         */
        private static int count = 0;

        /**
         * Ime labele.
         */
        public final String name;

        private Label(String name) {
            this.name = name;
        }

        /**
         * Ustvari novo anonimno labelo.
         */
        public static Label nextAnonymous() {
            return new Label("L[" + count++ + "]");
        }

        /**
         * Ustvari novo poimenovano labelo.
         */
        public static Label named(String name) {
            requireNonNull(name);
            return new Label(name);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Label t) && t.name.equals(this.name);
        }
    
        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /**
     * Graditelj klicnih zapisov.
     *
     * Abstrakcija, s pomočjo katere sestavimo
     * klicni zapis v več korakih.
     */
    public static class Builder {
        public final Label label;
        public final int staticLevel;
        private int parametersSize;
        private int argumentsSize;
        private int localsSize;

        public Builder(Label label, int staticLevel) {
            this.label = label;
            this.staticLevel = staticLevel;
        }

        /**
         * Ustvari nov klicni zapis.
         */
        public Frame build() {
            return new Frame(label, staticLevel, parametersSize, argumentsSize, localsSize);
        }

        /**
         * Dodaj parameter.
         * 
         * @param size velikost, ki jo parameter zasede na skladu
         * @return odmik dodanega parametra od FP
         */
        public int addParameter(int size) {
            var currentSize = parametersSize;
            parametersSize += size;
            return currentSize;
        }

        /**
         * Dodaj funkcijski klic. Velikost izhodnih argumentov
         * je največja izmed velikosti vseh klicev.
         */
        public void addFunctionCall(int argumentsSize) {
            this.argumentsSize = Math.max(this.argumentsSize, argumentsSize);
        }

        /**
         * Dodaj lokalno spremenljivko.
         *
         * @param size velikost lokalne spremenljivke na skladu
         * @return odmik lokalne spremenljivke od FP
         */
        public int addLocalVariable(int size) {
            var currentSize = localsSize;
            localsSize += size;
            return -currentSize - size;
        }
    }
}

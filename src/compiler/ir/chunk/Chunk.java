/**
 * @ Author: turk
 * @ Description: Fragment programa.
 */

package compiler.ir.chunk;

import static common.RequireNonNull.requireNonNull;

import compiler.frm.Access;
import compiler.frm.Frame;
import compiler.ir.code.stmt.IRStmt;

public abstract class Chunk {
    /**
     * Fragment globalne spremenljivke.
     */
    public static class GlobalChunk extends Chunk {
        public final Access.Global access;

        public GlobalChunk(Access.Global access) {
            requireNonNull(access);
            this.access = access;
        }
    }

    /**
     * Podatkovni fragment (do njega dostopamo enako
     * kot do globalne spremenljivke).
     */
    public static class DataChunk extends GlobalChunk {
        public final String data;

        public DataChunk(Access.Global access, String data) {
            super(access);
            requireNonNull(data);
            this.data = data;
        }
    }

    /**
     * Fragment kode.
     */
    public static class CodeChunk extends Chunk {
        /**
         * Klicni zapis funkcije.
         */
        public final Frame frame;

        /**
         * Koda funkcije.
         */
        public final IRStmt code;

        public CodeChunk(Frame frame, IRStmt code) {
            requireNonNull(frame, code);
            this.frame = frame;
            this.code = code;
        }

        @Override
        public String toString() {
            return frame.toString();
        }
    }
}

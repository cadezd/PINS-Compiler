/**
 * @Author: turk
 * @Description: Razred, namenjen obveščanju o poteku prevajanja.
 */

package common;

import java.io.PrintStream;

import compiler.lexer.Position;

/**
 * Obveščanje o napakah.
 */
public class Report {
    /**
     * NE SPREMINJAJ!
     */
    private static final int exitErrorCode = 99;

    /**
     * Izhodni tok, kamor se izpišejo napake.
     */
    public static PrintStream err = System.err;

    private Report() {}

    public static void error(String message) {
        err.println(message);
        System.exit(exitErrorCode);      
    }

    public static void error(Position position, String message) {
        err.println(position.toString() + ": " + message);
        System.exit(exitErrorCode);
    }
}

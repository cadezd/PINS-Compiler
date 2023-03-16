/**
 * @ Author: turk
 * @ Description:
 */

package compiler.common;

import static compiler.common.RequireNonNull.requireNonNull;

public class StringUtil {
    /**
     * Kreira nov indentiran niz.
     */
    public static String indented(String str, int indent) {
        requireNonNull(str);
        if (indent < 0) { throw new IllegalArgumentException("Indent must be at least 0!"); }
        var sb = new StringBuilder(indent);
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append(str);
        return sb.toString();
    }
}

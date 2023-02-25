/**
 * @ Author: turk
 * @ Description:
 */

package common;

import java.util.Objects;

public class RequireNonNull {
    public static void requireNonNull(Object... objects) {
        for (var obj : objects) {
            Objects.requireNonNull(obj);
        }
    }
}

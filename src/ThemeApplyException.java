/**
 * Thrown when a {@link Theme} cannot be applied — either because its LAF class
 * is missing from the classpath, or because instantiating / installing the
 * LAF fails. Callers should log and fall back; theming errors must never
 * propagate to the event-dispatch thread.
 */
public class ThemeApplyException extends Exception {
    public ThemeApplyException(String message, Throwable cause) {
        super(message, cause);
    }
}

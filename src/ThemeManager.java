import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Tracks the current light/dark theme preference and notifies listeners when
 * it changes. Actual component colors come from FlatLaf via UIManager — this
 * class only owns the on/off switch and the persistence of that choice.
 */
public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static final ThemeManager INSTANCE = new ThemeManager();
    private static final String PREF_KEY = "darkMode";

    private Theme current;
    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        // Dark-mode-first: default to dark when no saved preference exists.
        current = prefs.getBoolean(PREF_KEY, true) ? Theme.DARK : Theme.LIGHT;
    }

    public static ThemeManager get() {
        return INSTANCE;
    }

    public Theme current() {
        return current;
    }

    public boolean isDark() {
        return current == Theme.DARK;
    }

    /** Toggles between light and dark, persists preference, notifies listeners. */
    public void toggle() {
        current = isDark() ? Theme.LIGHT : Theme.DARK;
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        prefs.putBoolean(PREF_KEY, isDark());
        for (Runnable r : listeners) r.run();
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }
}

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Owns the active {@link Theme} and the curated preset list. Singleton with
 * a lazy lifecycle so {@link #setPreferencesNodeForTesting(Preferences)} can
 * redirect persistence before the instance is built.
 *
 * Public API: {@link #get()}, {@link #current()}, {@link #presets()},
 * {@link #isDark()}, {@link #addListener(Runnable)}.
 */
public final class ThemeManager {

    private static final String KEY_THEME    = "themeId";
    private static final String KEY_DARKMODE = "darkMode"; // legacy, read-only

    private static final List<Theme> PRESETS = Collections.unmodifiableList(Arrays.asList(
            new Theme("default-dark",    "Default Dark",
                    "com.formdev.flatlaf.FlatDarkLaf",
                    new Color(99, 102, 241), true),
            new Theme("default-light",   "Default Light",
                    "com.formdev.flatlaf.FlatLightLaf",
                    new Color(99, 102, 241), false),
            new Theme("github-light",    "GitHub Light",
                    "com.formdev.flatlaf.intellijthemes.FlatGitHubIJTheme",
                    null, false),
            new Theme("solarized-dark",  "Solarized Dark",
                    "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme",
                    null, true),
            new Theme("solarized-light", "Solarized Light",
                    "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme",
                    null, false),
            new Theme("dracula",         "Dracula",
                    "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme",
                    null, true),
            new Theme("nord",            "Nord",
                    "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme",
                    null, true),
            new Theme("one-dark",        "One Dark",
                    "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme",
                    null, true),
            // FlatLaf class name has a typo ("Monocai" instead of "Monokai") —
            // do not "fix" or the theme will fail to load.
            new Theme("monokai-pro",     "Monokai Pro",
                    "com.formdev.flatlaf.intellijthemes.FlatMonocaiProIJTheme",
                    null, true)
    ));

    private static volatile Preferences prefsNode = Preferences.userNodeForPackage(ThemeManager.class);
    private static volatile ThemeManager instance;

    private Theme current;
    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {
        this.current = defaultTheme();
    }

    /** Test-only seam — redirects prefs reads/writes and forces the singleton to rebuild. */
    static void setPreferencesNodeForTesting(Preferences node) {
        prefsNode = node;
        instance = null;
    }

    public static synchronized ThemeManager get() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public List<Theme> presets() {
        return PRESETS;
    }

    public Theme current() {
        return current;
    }

    public boolean isDark() {
        return current.isDark();
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    static Theme defaultTheme() {
        return findById("default-dark");
    }

    static Theme findById(String id) {
        for (Theme t : PRESETS) if (t.id().equals(id)) return t;
        return null;
    }
}

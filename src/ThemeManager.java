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
                    "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme",
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
            new Theme("monokai-pro",     "Monokai Pro",
                    "com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme",
                    null, true)
    ));

    private static volatile Preferences prefsNode = Preferences.userNodeForPackage(ThemeManager.class);
    private static volatile ThemeManager instance;

    private Theme current;
    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {
        this.current = resolveStartupTheme();
    }

    private static Theme resolveStartupTheme() {
        // 1. New key wins.
        String savedId = prefsNode.get(KEY_THEME, null);
        if (savedId != null) {
            Theme found = findById(savedId);
            if (found != null) return found;
            // Unknown id → fall through to default WITHOUT overwriting the saved value.
            return defaultTheme();
        }

        // 2. Migrate legacy boolean if present. Sentinel value distinguishes
        //    "key absent" from "key present and false".
        final boolean LEGACY_ABSENT_SENTINEL = true; // read with fake default = true
        boolean legacyTrue  = prefsNode.getBoolean(KEY_DARKMODE, LEGACY_ABSENT_SENTINEL);
        boolean legacyFalse = prefsNode.getBoolean(KEY_DARKMODE, false);
        boolean legacyKeyExists = legacyTrue == legacyFalse; // both reads return same value
        if (legacyKeyExists) {
            String migratedId = legacyTrue ? "default-dark" : "default-light";
            prefsNode.put(KEY_THEME, migratedId);
            return findById(migratedId);
        }

        // 3. No prefs → default-dark.
        return defaultTheme();
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

    public void setCurrent(Theme t) {
        this.current = t;
        prefsNode.put(KEY_THEME, t.id());
        try {
            t.apply();
        } catch (ThemeApplyException e) {
            // Persist user intent even if applying fails this session — next
            // launch will retry. Listeners still fire so border-rebuild etc.
            // re-runs against whatever LAF is active.
            System.err.println("Could not apply theme " + t.id() + ": " + e.getMessage());
        }
        for (Runnable r : listeners) r.run();
    }

    static Theme defaultTheme() {
        return findById("default-dark");
    }

    static Theme findById(String id) {
        for (Theme t : PRESETS) if (t.id().equals(id)) return t;
        return null;
    }
}

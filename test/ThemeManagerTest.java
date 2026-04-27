import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThemeManager}. Each test gets a fresh transient
 * Preferences sub-node so the developer's real prefs are never touched.
 */
public class ThemeManagerTest {

    private Preferences testNode;

    @BeforeEach
    void setUpPrefs() {
        testNode = Preferences.userRoot().node("at-test-" + UUID.randomUUID());
        ThemeManager.setPreferencesNodeForTesting(testNode);
    }

    @AfterEach
    void tearDownPrefs() throws Exception {
        testNode.removeNode();
        ThemeManager.setPreferencesNodeForTesting(
                Preferences.userNodeForPackage(ThemeManager.class));
    }

    @Test
    void presets_isNonEmpty() {
        List<Theme> ps = ThemeManager.get().presets();
        assertFalse(ps.isEmpty());
    }

    @Test
    void presets_containsBothDefaults() {
        List<Theme> ps = ThemeManager.get().presets();
        assertTrue(ps.contains(new Theme("default-dark", "", "", null, true)));
        assertTrue(ps.contains(new Theme("default-light", "", "", null, false)));
    }

    @Test
    void presets_containsAllNineExpectedIds() {
        List<String> ids = new java.util.ArrayList<>();
        for (Theme t : ThemeManager.get().presets()) ids.add(t.id());
        assertEquals(java.util.Arrays.asList(
                "default-dark", "default-light", "github-light",
                "solarized-dark", "solarized-light", "dracula",
                "nord", "one-dark", "monokai-pro"), ids);
    }

    @Test
    void startup_savedThemeId_isLoaded() {
        testNode.put("themeId", "dracula");
        ThemeManager.setPreferencesNodeForTesting(testNode); // forces rebuild
        assertEquals("dracula", ThemeManager.get().current().id());
    }

    @Test
    void startup_noPrefs_defaultsToDefaultDark() {
        // testNode is fresh and empty; rebuilt in @BeforeEach
        assertEquals("default-dark", ThemeManager.get().current().id());
    }

    @Test
    void migration_legacyDarkModeTrue_mapsToDefaultDark() {
        testNode.putBoolean("darkMode", true);
        ThemeManager.setPreferencesNodeForTesting(testNode);
        assertEquals("default-dark", ThemeManager.get().current().id());
        assertEquals("default-dark", testNode.get("themeId", null),
                "themeId should be persisted after migration");
    }

    @Test
    void migration_legacyDarkModeFalse_mapsToDefaultLight() {
        testNode.putBoolean("darkMode", false);
        ThemeManager.setPreferencesNodeForTesting(testNode);
        assertEquals("default-light", ThemeManager.get().current().id());
        assertEquals("default-light", testNode.get("themeId", null));
    }

    @Test
    void migration_runsOnlyOnce_subsequentLoadsUseThemeId() {
        // First load migrates.
        testNode.putBoolean("darkMode", true);
        ThemeManager.setPreferencesNodeForTesting(testNode);
        assertEquals("default-dark", ThemeManager.get().current().id());

        // Now manually flip darkMode to false; themeId should still win.
        testNode.putBoolean("darkMode", false);
        ThemeManager.setPreferencesNodeForTesting(testNode);
        assertEquals("default-dark", ThemeManager.get().current().id(),
                "themeId should override the legacy darkMode key");
    }

    @Test
    void bogusThemeId_fallsBackToDefault_doesNotOverwriteSavedValue() {
        testNode.put("themeId", "no-such-theme");
        ThemeManager.setPreferencesNodeForTesting(testNode);
        assertEquals("default-dark", ThemeManager.get().current().id());
        assertEquals("no-such-theme", testNode.get("themeId", null),
                "saved themeId must not be overwritten on fallback");
    }

    @Test
    void setCurrent_persistsThemeId() {
        Theme dracula = ThemeManager.findById("dracula");
        ThemeManager.get().setCurrent(dracula);
        assertEquals("dracula", testNode.get("themeId", null));
        assertEquals("dracula", ThemeManager.get().current().id());
    }

    @Test
    void setCurrent_firesListeners() {
        int[] callCount = {0};
        ThemeManager.get().addListener(() -> callCount[0]++);
        ThemeManager.get().setCurrent(ThemeManager.findById("nord"));
        assertEquals(1, callCount[0]);
    }

    @Test
    void setCurrent_isDarkReflectsCurrent() {
        ThemeManager.get().setCurrent(ThemeManager.findById("default-light"));
        assertFalse(ThemeManager.get().isDark());
        ThemeManager.get().setCurrent(ThemeManager.findById("default-dark"));
        assertTrue(ThemeManager.get().isDark());
    }
}

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
}

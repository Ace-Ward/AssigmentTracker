import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.junit.jupiter.api.*;

import javax.swing.UIManager;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Theme} — equality semantics and the apply() side effects
 * we can observe headlessly (UIManager state).
 */
public class ThemeTest {

    @AfterEach
    void resetLaf() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        UIManager.put("@accentColor", null);
    }

    @Test
    void equals_keyedOnId_sameId_areEqual() {
        Theme a = new Theme("x", "X", "com.formdev.flatlaf.FlatLightLaf", null, false);
        Theme b = new Theme("x", "Different Display", "com.formdev.flatlaf.FlatDarkLaf",
                new Color(1, 2, 3), true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentId_notEqual() {
        Theme a = new Theme("x", "X", "com.formdev.flatlaf.FlatLightLaf", null, false);
        Theme b = new Theme("y", "X", "com.formdev.flatlaf.FlatLightLaf", null, false);
        assertNotEquals(a, b);
    }

    @Test
    void getters_returnConstructorValues() {
        Color accent = new Color(99, 102, 241);
        Theme t = new Theme("default-dark", "Default Dark",
                "com.formdev.flatlaf.FlatDarkLaf", accent, true);
        assertEquals("default-dark", t.id());
        assertEquals("Default Dark", t.displayName());
        assertEquals("com.formdev.flatlaf.FlatDarkLaf", t.lafClassName());
        assertEquals(accent, t.accent());
        assertTrue(t.isDark());
    }

    @Test
    void apply_validClass_setsLookAndFeel() throws Exception {
        Theme t = new Theme("default-light", "Default Light",
                "com.formdev.flatlaf.FlatLightLaf", null, false);
        t.apply();
        assertTrue(UIManager.getLookAndFeel() instanceof FlatLightLaf,
                "expected FlatLightLaf, got " + UIManager.getLookAndFeel().getClass());
    }

    @Test
    void apply_withAccent_putsAccentColorInUIManager() throws Exception {
        Color accent = new Color(99, 102, 241);
        Theme t = new Theme("default-dark", "Default Dark",
                "com.formdev.flatlaf.FlatDarkLaf", accent, true);
        t.apply();
        assertEquals(accent, UIManager.get("@accentColor"));
    }

    @Test
    void apply_withNullAccent_clearsAccentColor() throws Exception {
        UIManager.put("@accentColor", new Color(255, 0, 0));
        Theme t = new Theme("nord", "Nord",
                "com.formdev.flatlaf.FlatDarkLaf", null, true);
        t.apply();
        assertNull(UIManager.get("@accentColor"));
    }

    @Test
    void apply_bogusClass_throwsThemeApplyException() {
        Theme t = new Theme("bogus", "Bogus",
                "com.formdev.flatlaf.DoesNotExistLaf", null, true);
        ThemeApplyException ex = assertThrows(ThemeApplyException.class, t::apply);
        assertNotNull(ex.getCause());
    }

    @Test
    void apply_classExistsButIsNotLookAndFeel_throwsThemeApplyException() {
        // java.lang.String exists but is not a LookAndFeel — the cast fails.
        Theme t = new Theme("not-a-laf", "Not LAF", "java.lang.String", null, true);
        ThemeApplyException ex = assertThrows(ThemeApplyException.class, t::apply);
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof ClassCastException,
                "expected ClassCastException cause, got " + ex.getCause().getClass());
    }
}

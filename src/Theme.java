import com.formdev.flatlaf.FlatLaf;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.Objects;

/**
 * Immutable description of a Look-and-Feel preset. Theme is identified by its
 * {@code id} (used as the persistence key). {@link #apply()} installs the LAF
 * by reflection on {@link #lafClassName()} and writes the optional accent
 * override into {@code UIManager}.
 */
public final class Theme {
    private final String id;
    private final String displayName;
    private final String lafClassName;
    private final Color accent;
    private final boolean dark;

    public Theme(String id, String displayName, String lafClassName,
                 Color accent, boolean dark) {
        this.id = id;
        this.displayName = displayName;
        this.lafClassName = lafClassName;
        this.accent = accent;
        this.dark = dark;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String lafClassName() { return lafClassName; }
    public Color accent() { return accent; }
    public boolean isDark() { return dark; }

    /**
     * Two Theme instances with the same {@code id} are considered equal even
     * if their accent / displayName differ. {@code id} is the persistence key.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Theme)) return false;
        return id.equals(((Theme) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Install this theme's LAF, write its accent override (or clear it when
     * {@code accent} is null), then refresh all open windows.
     *
     * @throws ThemeApplyException if the LAF class is missing or installation fails
     */
    public void apply() throws ThemeApplyException {
        try {
            LookAndFeel laf = (LookAndFeel) Class.forName(lafClassName)
                    .getDeclaredConstructor().newInstance();
            // Always set @accentColor — passing null removes the key, so the
            // next FlatLaf refresh derives the accent from LAF defaults.
            UIManager.put("@accentColor", accent);
            UIManager.setLookAndFeel(laf);
            FlatLaf.updateUI();
        } catch (ReflectiveOperationException | ClassCastException
               | javax.swing.UnsupportedLookAndFeelException e) {
            throw new ThemeApplyException("Could not apply theme " + id + ": " + e.getMessage(), e);
        }
    }
}

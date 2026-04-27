import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * Non-modal Preferences dialog containing a theme picker. Selection applies
 * live via {@link ThemeManager#setCurrent(Theme)}; closing the dialog
 * persists nothing extra.
 */
public final class PreferencesDialog extends JDialog {

    private static WeakReference<PreferencesDialog> activeRef = new WeakReference<>(null);

    /**
     * Show the dialog, bringing the existing instance to front if it's
     * already visible (preserves scroll / selection).
     */
    public static void show(Window owner) {
        PreferencesDialog existing = activeRef.get();
        if (existing != null && existing.isDisplayable() && existing.isVisible()) {
            existing.toFront();
            existing.requestFocus();
            return;
        }
        PreferencesDialog dlg = new PreferencesDialog(owner);
        activeRef = new WeakReference<>(dlg);
        dlg.setVisible(true);
    }

    private PreferencesDialog(Window owner) {
        super(owner, "Preferences", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(360, 420);
        setLocationRelativeTo(owner);

        JComponent themeSection = buildThemeSection();

        JButton close = new JButton("Close");
        close.putClientProperty("JButton.buttonType", "roundRect");
        close.addActionListener(e -> dispose());

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttonBar.add(close);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        content.add(themeSection, BorderLayout.CENTER);
        content.add(buttonBar, BorderLayout.SOUTH);

        setContentPane(content);
    }

    /**
     * Self-contained — when a future "Colors" or "Fonts" section lands,
     * wrap this in a JTabbedPane.
     */
    private JComponent buildThemeSection() {
        JLabel title = new JLabel("Theme");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        DefaultListModel<Theme> model = new DefaultListModel<>();
        for (Theme t : ThemeManager.get().presets()) model.addElement(t);

        JList<Theme> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ThemeCellRenderer());
        list.setSelectedValue(ThemeManager.get().current(), true);
        list.addListSelectionListener((ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting()) return;
            Theme selected = list.getSelectedValue();
            if (selected != null && !selected.equals(ThemeManager.get().current())) {
                ThemeManager.get().setCurrent(selected);
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(
                UIManager.getColor("Component.borderColor")));

        JPanel section = new JPanel(new BorderLayout());
        section.add(title, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    /** Renders each row as [swatch] displayName. */
    private static final class ThemeCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Theme) {
                Theme t = (Theme) value;
                setText("    " + t.displayName());
                setIcon(new SwatchIcon(swatchColor(t)));
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            }
            return this;
        }

        private static Color swatchColor(Theme t) {
            if (t.accent() != null) return t.accent();
            // No accent override → use a neutral indicator based on light/dark.
            return t.isDark() ? new Color(60, 60, 60) : new Color(220, 220, 220);
        }
    }

    private static final class SwatchIcon implements Icon {
        private final Color color;
        SwatchIcon(Color color) { this.color = color; }
        public int getIconWidth()  { return 12; }
        public int getIconHeight() { return 12; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, getIconWidth(), getIconHeight(), 4, 4);
            g2.setColor(new Color(0, 0, 0, 60));
            g2.drawRoundRect(x, y, getIconWidth() - 1, getIconHeight() - 1, 4, 4);
            g2.dispose();
        }
    }
}

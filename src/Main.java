import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Main class with main method invoked on app start.
 * @version 1.0.4
 * @author Dr. Jody Paul
 * @author Ayslynn Wardall, Kenneth Pyron, Amadou Seck
 */
public class Main {
    /** Private constructor to prevent instantiation of entry point class. */
    private Main() { }

    /**
     * Invoked on start.
     * @param args ignored
     */
    public static void main(String[] args) {
        // Install FlatLaf before any Swing component is created so it picks up
        // the modern Look & Feel defaults (fonts, borders, scrollbars, etc.).
        // Choice of dark vs. light follows the saved ThemeManager preference.
        if (ThemeManager.get().isDark()) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    /** Draws a small calendar icon at the given size using Java2D. */
    private static ImageIcon makeCalendarIcon(int size) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = size / 5;
        // Body
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 2, size, size - 2, arc, arc);
        // Header bar
        g.setColor(new Color(80, 120, 200));
        g.fillRoundRect(0, 2, size, size / 3, arc, arc);
        g.fillRect(0, size / 3 - arc / 2, size, arc / 2 + 1); // square off bottom of header
        // Border
        g.setColor(new Color(100, 100, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 2, size - 1, size - 3, arc, arc);
        // Ring pegs at top
        g.setColor(new Color(60, 60, 60));
        int pegY = 0;
        int pegW = Math.max(2, size / 8);
        int pegH = size / 4;
        g.fillRoundRect(size / 4 - pegW / 2, pegY, pegW, pegH, 2, 2);
        g.fillRoundRect(3 * size / 4 - pegW / 2, pegY, pegW, pegH, 2, 2);
        // Grid dots (3x2)
        g.setColor(new Color(80, 120, 200));
        int dotSize = Math.max(1, size / 10);
        int gridTop = size / 3 + size / 8;
        int rowH = (size - gridTop - 2) / 2;
        int colW = size / 3;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int dx = colW * col + colW / 2 - dotSize / 2;
                int dy = gridTop + row * rowH + rowH / 2 - dotSize / 2;
                g.fillRect(dx, dy, dotSize + 1, dotSize + 1);
            }
        }
        g.dispose();
        return new ImageIcon(img);
    }

    private static void createAndShowGUI() {
        ThemeManager theme = ThemeManager.get();

        JFrame frame = new JFrame("Assignment Tracker");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Default size sized so the full input row (assignment, due date,
        // buttons, and dark-mode toggle) fits without wrapping.
        frame.setSize(1100, 650);
        frame.setLocationRelativeTo(null);

        // --- Data models ---
        DefaultListModel<Subject> subjectListModel = new DefaultListModel<>();
        DataStore.load(subjectListModel);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DataStore.save(subjectListModel);
                frame.dispose();
            }
        });
        DefaultTableModel emptyTableModel = new DefaultTableModel(
                new String[]{"Assignment", "Due Date", "Done"}, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 2 ? Boolean.class : String.class; // col 2 = "Done" checkbox
            }
        };

        // --- Center: assignment table ---
        // Colors come from the active FlatLaf theme via UIManager; only
        // layout/size tweaks are set here.
        JTable table = new JTable(emptyTableModel);
        table.setRowHeight(24);

        // --- South: input panel (disabled until a class is selected) ---
        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        // dateField shows MM/dd/yyyy to the user but is read-only; picker button sets it
        JTextField nameField = new JTextField(20);
        JTextField dateField = new JTextField(10);
        dateField.setEditable(false);
        dateField.setToolTipText("Click 📅 to pick a date");
        JButton datePickerButton = new JButton(makeCalendarIcon(16));
        datePickerButton.setToolTipText("Pick a date");
        datePickerButton.setFocusPainted(false);
        // Holds the date in MM/dd/yyyy format; empty string when unset
        final LocalDate[] pickedDate = {null};
        datePickerButton.addActionListener(e -> {
            LocalDate initial = pickedDate[0] != null ? pickedDate[0] : LocalDate.now();
            LocalDate chosen = DatePickerDialog.show(datePickerButton, initial);
            if (chosen != null) {
                pickedDate[0] = chosen;
                dateField.setText(chosen.format(displayFmt));
            }
        });
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove Selected");
        nameField.setEnabled(false);
        dateField.setEnabled(false);
        datePickerButton.setEnabled(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        JLabel darkModeLabel = new JLabel(theme.isDark() ? "Light Mode" : "Dark Mode");
        JToggleButton darkModeToggle = new JToggleButton(theme.isDark() ? "☀" : "☾");
        darkModeToggle.setSelected(theme.isDark());
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setToolTipText("Toggle dark/light mode");

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Assignment:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Due Date:"));
        inputPanel.add(dateField);
        inputPanel.add(datePickerButton);
        inputPanel.add(addButton);
        inputPanel.add(removeButton);

        // Spacer to push toggle to the right
        inputPanel.add(Box.createHorizontalStrut(20));
        inputPanel.add(darkModeLabel);
        inputPanel.add(darkModeToggle);

        // --- West: sidebar ---
        JList<Subject> subjectList = new JList<>(subjectListModel);
        subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectList.setFont(subjectList.getFont().deriveFont(13f));

        // Sidebar action buttons are icon-only (monochrome Unicode glyphs so
        // FlatLaf handles the color automatically). Tooltips carry the
        // full label for accessibility.
        JButton newClassButton = new JButton("+");
        newClassButton.setToolTipText("New class");
        JButton renameButton = new JButton("\u270E"); // pencil
        renameButton.setToolTipText("Rename class");
        JButton deleteButton = new JButton("\u2715"); // heavy X
        deleteButton.setToolTipText("Delete class");
        // Bump the glyph size so the icons read clearly on smaller buttons.
        Font iconFont = newClassButton.getFont().deriveFont(18f);
        newClassButton.setFont(iconFont);
        renameButton.setFont(iconFont);
        deleteButton.setFont(iconFont);
        renameButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Horizontal row of equally-sized icon buttons.
        JPanel sidebarButtonPanel = new JPanel(new GridLayout(1, 3, 4, 0));
        sidebarButtonPanel.add(newClassButton);
        sidebarButtonPanel.add(renameButton);
        sidebarButtonPanel.add(deleteButton);

        JScrollPane sidebarScroll = new JScrollPane(subjectList);

        JPanel sidebarPanel = new JPanel(new BorderLayout(0, 6));
        sidebarPanel.setPreferredSize(new Dimension(210, 0));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        sidebarPanel.add(sidebarScroll, BorderLayout.CENTER);
        sidebarPanel.add(sidebarButtonPanel, BorderLayout.SOUTH);

        // --- Calendar panel ---
        // Built up here (before the action listeners below) so those listeners
        // can capture it and call refresh() when assignments change.
        CalendarPanel calendarPanel = new CalendarPanel(subjectListModel);

        // Swap the FlatLaf Look & Feel when the user toggles dark/light mode.
        // Registered FIRST so it runs before the color-override listener below
        // (which sets specific component colors on top of the LAF defaults).
        theme.addListener(() -> {
            try {
                UIManager.setLookAndFeel(theme.isDark()
                        ? new FlatDarkLaf() : new FlatLightLaf());
                FlatLaf.updateUI(); // repaint every open window with the new LAF
            } catch (UnsupportedLookAndFeelException ex) {
                System.err.println("Could not switch LAF: " + ex.getMessage());
            }
        });

        // --- Dark mode toggle action ---
        darkModeToggle.addActionListener(e -> theme.toggle());

        // --- Selection listener: swap table model, toggle controls ---
        subjectList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Subject selected = subjectList.getSelectedValue();
            boolean subjectSelected = selected != null;
            table.setModel(subjectSelected ? selected.getTableModel() : emptyTableModel);
            nameField.setEnabled(subjectSelected);
            dateField.setEnabled(subjectSelected);
            datePickerButton.setEnabled(subjectSelected);
            addButton.setEnabled(subjectSelected);
            removeButton.setEnabled(subjectSelected);
            renameButton.setEnabled(subjectSelected);
            deleteButton.setEnabled(subjectSelected);
        });

        // --- Assignment actions ---
        // Each listener calls getSelectedValue() at the moment it fires, rather than
        // capturing the selection up front. This ensures the selection is always current.
        addButton.addActionListener(e -> {
            Subject selected = subjectList.getSelectedValue();
            if (selected == null) return;
            String name = nameField.getText().trim();
            String date = dateField.getText().trim(); // already MM/dd/yyyy or empty
            if (!name.isEmpty()) {
                selected.getTableModel().addRow(new Object[]{name, date, false});
                nameField.setText("");
                dateField.setText("");
                pickedDate[0] = null;
                nameField.requestFocus();
                calendarPanel.refresh(); // keep due-date markers in sync
            }
        });

        // --- Remove assignment ---
        removeButton.addActionListener(e -> {
            Subject selected = subjectList.getSelectedValue();
            if (selected == null) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                selected.getTableModel().removeRow(row);
                calendarPanel.refresh(); // keep due-date markers in sync
            }
        });

        // --- New Class ---
        newClassButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(frame, "Class name:", "New Class",
                    JOptionPane.PLAIN_MESSAGE);
            if (input == null) return;
            String name = input.trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Class name cannot be empty.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < subjectListModel.size(); i++) {
                if (subjectListModel.get(i).getName().equalsIgnoreCase(name)) {
                    JOptionPane.showMessageDialog(frame, "A class with that name already exists.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            Subject subject = new Subject(name);
            subjectListModel.addElement(subject);
            subjectList.setSelectedValue(subject, true);
        });

        // --- Rename ---
        renameButton.addActionListener(e -> {
            Subject selected = subjectList.getSelectedValue();
            if (selected == null) return;
            String input = (String) JOptionPane.showInputDialog(frame, "New name:",
                    "Rename Class", JOptionPane.PLAIN_MESSAGE, null, null, selected.getName());
            if (input == null) return;
            String name = input.trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Class name cannot be empty.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < subjectListModel.size(); i++) {
                Subject s = subjectListModel.get(i);
                if (s != selected && s.getName().equalsIgnoreCase(name)) {
                    JOptionPane.showMessageDialog(frame, "A class with that name already exists.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            int index = subjectList.getSelectedIndex();
            selected.setName(name);
            subjectListModel.set(index, selected);
            calendarPanel.refresh(); // tooltip labels include the subject name
        });

        // --- Delete ---
        deleteButton.addActionListener(e -> {
            Subject selected = subjectList.getSelectedValue();
            if (selected == null) return;
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete '" + selected.getName() + "' and all its assignments? This cannot be undone.",
                    "Delete Class", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            int index = subjectList.getSelectedIndex();
            subjectListModel.remove(index);
            if (!subjectListModel.isEmpty()) {
                subjectList.setSelectedIndex(Math.max(0, index - 1));
            }
            calendarPanel.refresh(); // deleted subject's assignments come off the calendar
            // When the list is empty, removing the last element clears the JList
            // selection automatically, which fires the ListSelectionListener above.
            // That listener resets the table and disables all controls.
        });

        // --- Assemble frame ---
        JScrollPane tableScroll = new JScrollPane(table);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Assignments", tableScroll);
        tabs.addTab("Calendar", calendarPanel);

        // Component colors are handled by the FlatLaf LAF swap registered
        // above. This listener only updates text that reflects the current
        // mode (the sun/moon icon and the label next to it).
        theme.addListener(() -> {
            darkModeToggle.setText(theme.isDark() ? "☀" : "☾");
            darkModeLabel.setText(theme.isDark() ? "Light Mode" : "Dark Mode");
        });

        // Redraw the calendar whenever the user switches to its tab so any
        // assignments added/removed on the Assignments tab show up.
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == calendarPanel) {
                calendarPanel.refresh();
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(sidebarPanel, BorderLayout.WEST);
        frame.add(tabs, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}

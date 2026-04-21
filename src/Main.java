import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Main class with main method invoked on app start.
 * @version 1.0.4
 * @author Dr. Jody Paul
 * @author Ayslynn Wardall, Kenneth Pyron, Amadou Seck, Diego Salas
 */
public class Main {
    /** Private constructor to prevent instantiation of entry point class. */
    private Main() { }

    /**
     * Invoked on start.
     * @param args ignored
     */
    public static void main(String[] args) {
        UIManager.put("@accentColor", new Color(99, 102, 241));
        if (ThemeManager.get().isDark()) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("Component.arc", 6);
        Font tnr = new Font("Times New Roman", Font.PLAIN, 15);
        for (String key : new String[]{
                "Button.font", "Label.font", "List.font", "Table.font",
                "TableHeader.font", "TextField.font", "TextArea.font",
                "ComboBox.font", "TabbedPane.font", "ToggleButton.font",
                "TitledBorder.font", "ToolTip.font"}) {
            UIManager.put(key, tnr);
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
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 2, size, size - 2, arc, arc);
        g.setColor(new Color(99, 102, 241));
        g.fillRoundRect(0, 2, size, size / 3, arc, arc);
        g.fillRect(0, size / 3 - arc / 2, size, arc / 2 + 1);
        g.setColor(new Color(100, 100, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 2, size - 1, size - 3, arc, arc);
        g.setColor(new Color(60, 60, 60));
        int pegY = 0;
        int pegW = Math.max(2, size / 8);
        int pegH = size / 4;
        g.fillRoundRect(size / 4 - pegW / 2, pegY, pegW, pegH, 2, 2);
        g.fillRoundRect(3 * size / 4 - pegW / 2, pegY, pegW, pegH, 2, 2);
        g.setColor(new Color(99, 102, 241));
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
        frame.setSize(1140, 680);
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
                new String[]{"Assignment", "Due Date", "Done", "Notes"}, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 2 ? Boolean.class : String.class;
            }
        };

        // --- Center: assignment table ---
        JTable table = new JTable(emptyTableModel);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setFont(
            table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(emptyTableModel);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(3).setMinWidth(0);
        table.getColumnModel().getColumn(3).setMaxWidth(0);
        table.getColumnModel().getColumn(3).setWidth(0);

        NotesPanel notesPanel = new NotesPanel(subjectListModel, table);

        // --- Filter bar ---
        JTextField filterField = new JTextField(18);
        filterField.setToolTipText("Search assignments or notes");
        filterField.putClientProperty("JTextField.placeholderText", "Search...");
        String[] filterOptions = {"All", "Has Notes", "No Notes", "Done", "Not Done"};
        JComboBox<String> filterCombo = new JComboBox<>(filterOptions);
        JButton clearFilterButton = new JButton("Clear");
        clearFilterButton.putClientProperty("JButton.buttonType", "roundRect");

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filterBar.add(filterField);
        filterBar.add(filterCombo);
        filterBar.add(clearFilterButton);

        Runnable applyFilter = () -> {
            String text = filterField.getText().trim().toLowerCase();
            String mode = (String) filterCombo.getSelectedItem();
            RowFilter<DefaultTableModel, Integer> filter = new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    String name  = entry.getStringValue(0).toLowerCase();
                    String notes = entry.getStringValue(3).toLowerCase();
                    boolean done = Boolean.TRUE.equals(entry.getValue(2));
                    boolean textMatch = text.isEmpty() || name.contains(text) || notes.contains(text);
                    boolean modeMatch;
                    if ("Has Notes".equals(mode)) {
                        modeMatch = !notes.trim().isEmpty();
                    } else if ("No Notes".equals(mode)) {
                        modeMatch = notes.trim().isEmpty();
                    } else if ("Done".equals(mode)) {
                        modeMatch = done;
                    } else if ("Not Done".equals(mode)) {
                        modeMatch = !done;
                    } else {
                        modeMatch = true;
                    }
                    return textMatch && modeMatch;
                }
            };
            sorter.setRowFilter(filter);
        };

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter.run(); }
        });
        filterCombo.addActionListener(e -> applyFilter.run());
        clearFilterButton.addActionListener(e -> {
            filterField.setText("");
            filterCombo.setSelectedIndex(0);
        });

        // --- South: input panel ---
        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        JTextField nameField = new JTextField(20);
        nameField.putClientProperty("JTextField.placeholderText", "Assignment name");
        JTextField dateField = new JTextField(10);
        dateField.setEditable(false);
        dateField.setToolTipText("Click the calendar button to pick a date");
        JButton datePickerButton = new JButton(makeCalendarIcon(16));
        datePickerButton.setToolTipText("Pick a date");
        datePickerButton.setFocusPainted(false);
        datePickerButton.putClientProperty("JButton.buttonType", "roundRect");

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
        addButton.putClientProperty("JButton.buttonType", "roundRect");
        addButton.setFocusPainted(false);

        JButton removeButton = new JButton("Remove");
        removeButton.putClientProperty("JButton.buttonType", "roundRect");
        removeButton.setFocusPainted(false);

        nameField.setEnabled(false);
        dateField.setEnabled(false);
        datePickerButton.setEnabled(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        JToggleButton darkModeToggle = new JToggleButton(theme.isDark() ? "☀" : "☽");
        darkModeToggle.setSelected(theme.isDark());
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setToolTipText("Toggle dark/light mode");
        darkModeToggle.putClientProperty("JButton.buttonType", "roundRect");

        JPanel inputLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        inputLeft.add(new JLabel("Assignment:"));
        inputLeft.add(nameField);
        inputLeft.add(new JLabel("Due Date:"));
        inputLeft.add(dateField);
        inputLeft.add(datePickerButton);
        inputLeft.add(addButton);
        inputLeft.add(removeButton);

        JPanel inputRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        inputRight.add(darkModeToggle);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        inputPanel.add(inputLeft, BorderLayout.CENTER);
        inputPanel.add(inputRight, BorderLayout.EAST);

        // --- West: sidebar ---
        JComboBox<Subject> subjectList = new JComboBox<>();
        for (int i = 0; i < subjectListModel.size(); i++) {
            subjectList.addItem(subjectListModel.get(i));
        }
        subjectListModel.addListDataListener(new javax.swing.event.ListDataListener() {
            public void intervalAdded(javax.swing.event.ListDataEvent e) {
                subjectList.removeAllItems();
                for (int i = 0; i < subjectListModel.size(); i++) subjectList.addItem(subjectListModel.get(i));
            }
            public void intervalRemoved(javax.swing.event.ListDataEvent e) {
                subjectList.removeAllItems();
                for (int i = 0; i < subjectListModel.size(); i++) subjectList.addItem(subjectListModel.get(i));
            }
            public void contentsChanged(javax.swing.event.ListDataEvent e) {
                subjectList.removeAllItems();
                for (int i = 0; i < subjectListModel.size(); i++) subjectList.addItem(subjectListModel.get(i));
            }
        });

        JButton newClassButton = new JButton("+");
        newClassButton.setToolTipText("New class");
        JButton renameButton = new JButton("✎");
        renameButton.setToolTipText("Rename class");
        JButton deleteButton = new JButton("✕");
        deleteButton.setToolTipText("Delete class");
        Font iconFont = newClassButton.getFont().deriveFont(Font.BOLD, 14f);
        for (JButton b : new JButton[]{newClassButton, renameButton, deleteButton}) {
            b.setFont(iconFont);
            b.putClientProperty("JButton.buttonType", "roundRect");
            b.setFocusPainted(false);
        }
        renameButton.setEnabled(false);
        deleteButton.setEnabled(false);

        JPanel sidebarButtonPanel = new JPanel(new GridLayout(1, 3, 6, 0));
        sidebarButtonPanel.setOpaque(false);
        sidebarButtonPanel.add(newClassButton);
        sidebarButtonPanel.add(renameButton);
        sidebarButtonPanel.add(deleteButton);


        // --- Calendar panel ---
        CalendarPanel calendarPanel = new CalendarPanel(subjectListModel);

        // Swap LAF on theme toggle.
        theme.addListener(() -> {
            try {
                UIManager.setLookAndFeel(theme.isDark()
                        ? new FlatDarkLaf() : new FlatLightLaf());
                FlatLaf.updateUI();
            } catch (UnsupportedLookAndFeelException ex) {
                System.err.println("Could not switch LAF: " + ex.getMessage());
            }
        });

        // --- Dark mode toggle action ---
        darkModeToggle.addActionListener(e -> theme.toggle());

        theme.addListener(() -> {
            darkModeToggle.setText(theme.isDark() ? "☀" : "☽");
            // Rebuild input border so it picks up the new border color.
            inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                    UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
            ));
        });

        // --- Selection listener: swap table model, toggle controls ---
        subjectList.addActionListener(e -> {
            Subject selected = (Subject) subjectList.getSelectedItem();
            boolean subjectSelected = selected != null;
            DefaultTableModel activeModel = subjectSelected ? selected.getTableModel() : emptyTableModel;
            table.setModel(activeModel);
            sorter.setModel(activeModel);
            table.setRowSorter(sorter);
            applyFilter.run();

            table.getColumnModel().getColumn(3).setMinWidth(0);
            table.getColumnModel().getColumn(3).setMaxWidth(0);
            table.getColumnModel().getColumn(3).setWidth(0);

            // Re-apply cell renderer after model swap.
            DefaultTableCellRenderer r = new DefaultTableCellRenderer();
            r.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            table.getColumnModel().getColumn(0).setCellRenderer(r);
            table.getColumnModel().getColumn(1).setCellRenderer(r);

            notesPanel.setSubject(selected);
            nameField.setEnabled(subjectSelected);
            dateField.setEnabled(subjectSelected);
            datePickerButton.setEnabled(subjectSelected);
            addButton.setEnabled(subjectSelected);
            removeButton.setEnabled(subjectSelected);
            renameButton.setEnabled(subjectSelected);
            deleteButton.setEnabled(subjectSelected);
        });

        // --- Assignment actions ---
        addButton.addActionListener(e -> {
            Subject selected = (Subject) subjectList.getSelectedItem();
            if (selected == null) return;
            String name = nameField.getText().trim();
            String date = dateField.getText().trim();
            if (!name.isEmpty()) {
                selected.getTableModel().addRow(new Object[]{name, date, false, ""});
                nameField.setText("");
                dateField.setText("");
                pickedDate[0] = null;
                nameField.requestFocus();
                calendarPanel.refresh();
                applyFilter.run();
            }
        });

        removeButton.addActionListener(e -> {
            Subject selected = (Subject) subjectList.getSelectedItem();
            if (selected == null) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                selected.getTableModel().removeRow(row);
                calendarPanel.refresh();
                applyFilter.run();
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
            subjectList.setSelectedItem(subject);
        });

        // --- Rename ---
        renameButton.addActionListener(e -> {
            Subject selected = (Subject) subjectList.getSelectedItem();
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
            subjectList.setSelectedItem(selected);
            calendarPanel.refresh();
        });

        // --- Delete ---
        deleteButton.addActionListener(e -> {
            Subject selected = (Subject) subjectList.getSelectedItem();
            if (selected == null) return;
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete '" + selected.getName() + "' and all its assignments? This cannot be undone.",
                    "Delete Class", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            int index = subjectList.getSelectedIndex();
            subjectListModel.remove(index);
            if (!subjectListModel.isEmpty()) {
                subjectList.setSelectedIndex(Math.max(0, index - 1));
                subjectList.setSelectedItem(subjectListModel.get(Math.max(0, index - 1)));
            }
            calendarPanel.refresh();
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(filterBar, BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        JSplitPane assignmentSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, notesPanel);
        assignmentSplit.setResizeWeight(0.75);
        assignmentSplit.setBorder(null);

        // --- App header bar ---
        JLabel appTitle = new JLabel("Assignment Tracker");
        appTitle.setFont(appTitle.getFont().deriveFont(Font.BOLD, 15f));

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerControls.setOpaque(false);
        headerControls.add(new JLabel("Class:"));
        headerControls.add(subjectList);
        headerControls.add(newClassButton);
        headerControls.add(renameButton);
        headerControls.add(deleteButton);

        JPanel headerBar = new JPanel(new BorderLayout(12, 0));
        headerBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0,
                UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        headerBar.add(appTitle, BorderLayout.WEST);
        headerBar.add(headerControls, BorderLayout.CENTER);

        theme.addListener(() -> {
            headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                    UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
        });

        // --- Scratchpad tab ---
        JTextArea scratchArea = new JTextArea(DataStore.loadScratchpad());
        scratchArea.setLineWrap(true);
        scratchArea.setWrapStyleWord(true);
        scratchArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JButton saveScratchButton = new JButton("Save Notes");
        saveScratchButton.putClientProperty("JButton.buttonType", "roundRect");
        saveScratchButton.setFocusPainted(false);
        saveScratchButton.addActionListener(e -> DataStore.saveScratchpad(scratchArea.getText()));

        JPanel scratchSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        scratchSouth.add(saveScratchButton);

        JPanel scratchPanel = new JPanel(new BorderLayout());
        scratchPanel.add(new JScrollPane(scratchArea), BorderLayout.CENTER);
        scratchPanel.add(scratchSouth, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Assignments", assignmentSplit);
        tabs.addTab("Calendar", calendarPanel);
        tabs.addTab("Notes", scratchPanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == calendarPanel) {
                calendarPanel.refresh();
            }
        });

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(headerBar, BorderLayout.NORTH);
        centerPanel.add(tabs, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Main class with main method invoked on app start.
 * @version 1.0.3
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
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Assignment Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 550);
        frame.setLocationRelativeTo(null);

        // --- Data models ---
        DefaultListModel<Subject> subjectListModel = new DefaultListModel<>();
        DefaultTableModel emptyTableModel = new DefaultTableModel(
                new String[]{"Assignment", "Due Date", "Done"}, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 2 ? Boolean.class : String.class; // col 2 = "Done" checkbox
            }
        };

        // --- Center: assignment table ---
        JTable table = new JTable(emptyTableModel);
        table.setRowHeight(24);

        // --- South: input panel (disabled until a class is selected) ---
        JTextField nameField = new JTextField(20);
        JTextField dateField = new JTextField(10);
        dateField.setToolTipText("e.g. 2026-04-10");
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove Selected");
        nameField.setEnabled(false);
        dateField.setEnabled(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Assignment:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Due Date:"));
        inputPanel.add(dateField);
        inputPanel.add(addButton);
        inputPanel.add(removeButton);

        // --- West: sidebar ---
        JList<Subject> subjectList = new JList<>(subjectListModel);
        subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectList.setBackground(new Color(44, 44, 44));
        subjectList.setForeground(Color.WHITE);
        subjectList.setSelectionBackground(new Color(70, 70, 70));
        subjectList.setSelectionForeground(Color.WHITE);
        subjectList.setFont(subjectList.getFont().deriveFont(13f));

        JButton newClassButton = new JButton("New Class");
        JButton renameButton = new JButton("Rename");
        JButton deleteButton = new JButton("Delete");
        renameButton.setEnabled(false);
        deleteButton.setEnabled(false);

        JPanel sidebarButtonPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        sidebarButtonPanel.add(newClassButton);
        sidebarButtonPanel.add(renameButton);
        sidebarButtonPanel.add(deleteButton);
        sidebarButtonPanel.setBackground(new Color(44, 44, 44));

        JScrollPane sidebarScroll = new JScrollPane(subjectList);
        sidebarScroll.getViewport().setBackground(new Color(44, 44, 44));

        JPanel sidebarPanel = new JPanel(new BorderLayout(0, 4));
        sidebarPanel.setPreferredSize(new Dimension(150, 0));
        sidebarPanel.setBackground(new Color(44, 44, 44));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        sidebarPanel.add(sidebarScroll, BorderLayout.CENTER);
        sidebarPanel.add(sidebarButtonPanel, BorderLayout.SOUTH);

        // --- Selection listener: swap table model, toggle controls ---
        subjectList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Subject selected = subjectList.getSelectedValue();
            boolean subjectSelected = selected != null;
            table.setModel(subjectSelected ? selected.getTableModel() : emptyTableModel);
            nameField.setEnabled(subjectSelected);
            dateField.setEnabled(subjectSelected);
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
            String date = dateField.getText().trim();
            if (!name.isEmpty()) {
                selected.getTableModel().addRow(new Object[]{name, date, false});
                nameField.setText("");
                dateField.setText("");
                nameField.requestFocus();
            }
        });

        // --- Remove assignment ---
        removeButton.addActionListener(e -> {
            Subject selected = subjectList.getSelectedValue();
            if (selected == null) return;
            int row = table.getSelectedRow();
            if (row >= 0) {
                selected.getTableModel().removeRow(row);
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
            // When the list is empty, removing the last element clears the JList
            // selection automatically, which fires the ListSelectionListener above.
            // That listener resets the table and disables all controls.
        });

        // --- Assemble frame ---
        frame.setLayout(new BorderLayout());
        frame.add(sidebarPanel, BorderLayout.WEST);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}

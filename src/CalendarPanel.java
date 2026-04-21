import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;

/**
 * Month-view calendar. Reads assignments from the subject list model and
 * highlights any date that has something due. All colors come from the
 * current Look & Feel via UIManager so the panel stays in sync with FlatLaf's
 * light/dark themes.
 */
public class CalendarPanel extends JPanel {

    private static final Color DUE_DOT = new Color(239, 68, 68);
    private static final Color ACCENT  = new Color(99, 102, 241);

    private YearMonth displayedMonth;

    private JLabel monthYearLabel;
    private JPanel gridPanel;

    /** Source of assignments; may be null for a standalone calendar. */
    private final DefaultListModel<Subject> subjectModel;

    /** Optional: no assignment data. */
    public CalendarPanel() {
        this(null);
    }

    /**
     * @param subjectModel list of subjects whose assignments should appear on the calendar
     */
    public CalendarPanel(DefaultListModel<Subject> subjectModel) {
        this.subjectModel = subjectModel;
        this.displayedMonth = YearMonth.now();
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        gridPanel = new JPanel();
        add(gridPanel, BorderLayout.CENTER);
        rebuildGrid();

        // Rebuild so UIManager colors are re-queried after a theme switch.
        // Deferred via invokeLater so the rebuild runs AFTER the LAF-swap
        // listener (and FlatLaf.updateUI) finishes — otherwise cells get
        // built with the old theme's UIResource colors, which Swing then
        // overwrites with the new LAF's Panel.background defaults during
        // updateUI, wiping the today highlight.
        ThemeManager.get().addListener(() -> SwingUtilities.invokeLater(this::rebuildGrid));
    }

    /** Rebuilds the grid so newly added/removed assignments show up. */
    public void refresh() {
        rebuildGrid();
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton prevButton = makeNavButton("\u25C0");
        JButton nextButton = makeNavButton("\u25B6");

        prevButton.addActionListener(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            rebuildGrid();
        });
        nextButton.addActionListener(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            rebuildGrid();
        });

        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(monthYearLabel.getFont().deriveFont(Font.BOLD, 17f));
        monthYearLabel.setForeground(ACCENT);

        h.add(prevButton,     BorderLayout.WEST);
        h.add(monthYearLabel, BorderLayout.CENTER);
        h.add(nextButton,     BorderLayout.EAST);
        return h;
    }

    private JButton makeNavButton(String symbol) {
        JButton btn = new JButton(symbol);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
        btn.setForeground(ACCENT);
        return btn;
    }

    private void rebuildGrid() {
        String monthName = displayedMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
        monthYearLabel.setText(monthName + " " + displayedMonth.getYear());

        Color gridLine = UIManager.getColor("Component.borderColor");

        remove(gridPanel);
        gridPanel = new JPanel(new GridLayout(0, 7, 1, 1));
        gridPanel.setBackground(gridLine);
        gridPanel.setBorder(BorderFactory.createLineBorder(gridLine));

        addDayNameRow();
        addDateCells(collectDueDates());

        add(gridPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void addDayNameRow() {
        Color bg = UIManager.getColor("TableHeader.background");
        Color fg = UIManager.getColor("TableHeader.foreground");
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String name : dayNames) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(bg);
            label.setForeground(fg);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
            label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            gridPanel.add(label);
        }
    }

    /**
     * Walks every subject and every row in its table model, parsing the
     * "Due Date" column into a LocalDate. Rows with an empty or unparseable
     * date are silently skipped — students shouldn't see a crash for a typo.
     *
     * @return map of date -> list of "Subject: Assignment" strings due that day
     */
    private Map<LocalDate, List<String>> collectDueDates() {
        Map<LocalDate, List<String>> byDate = new HashMap<>();
        if (subjectModel == null) return byDate;
        for (int i = 0; i < subjectModel.size(); i++) {
            Subject subject = subjectModel.get(i);
            DefaultTableModel dtm = subject.getTableModel();
            for (int r = 0; r < dtm.getRowCount(); r++) {
                Object dateCell = dtm.getValueAt(r, 1);
                Object nameCell = dtm.getValueAt(r, 0);
                if (!(dateCell instanceof String)) continue;
                String raw = ((String) dateCell).trim();
                if (raw.isEmpty()) continue;
                try {
                    LocalDate d = parseDate(raw);
                    if (d == null) continue;
                    String lbl = subject.getName() + ": " + nameCell;
                    byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(lbl);
                } catch (DateTimeParseException ignored) {
                    // Skip malformed dates rather than failing the whole render.
                }
            }
        }
        return byDate;
    }

    /**
     * Parses a date string in either MM/dd/yyyy or yyyy-MM-dd format.
     * Returns null if the string doesn't match either format.
     */
    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDate.parse(raw); // yyyy-MM-dd fallback for existing data
        } catch (DateTimeParseException ignored) { }
        return null;
    }

    private void addDateCells(Map<LocalDate, List<String>> dueByDate) {
        LocalDate today        = LocalDate.now();
        LocalDate firstOfMonth = displayedMonth.atDay(1);

        // DayOfWeek: MON=1..SUN=7. We want Sun=0..Sat=6 so grid starts on Sunday.
        int startOffset = firstOfMonth.getDayOfWeek().getValue() % 7;

        LocalDate cellDate = firstOfMonth.minusDays(startOffset);

        for (int i = 0; i < 42; i++) {
            boolean isThisMonth = YearMonth.from(cellDate).equals(displayedMonth);
            boolean isToday     = cellDate.equals(today);
            List<String> due    = dueByDate.get(cellDate);
            gridPanel.add(buildDateCell(cellDate, isThisMonth, isToday, due));
            cellDate = cellDate.plusDays(1);
        }
    }

    private JPanel buildDateCell(LocalDate date, boolean isThisMonth,
                                 boolean isToday, List<String> due) {
        Color thisMonthBg  = UIManager.getColor("Table.background");
        Color otherMonthBg = UIManager.getColor("Panel.background");
        Color todayBg      = UIManager.getColor("Table.selectionBackground");
        Color todayFg      = UIManager.getColor("Table.selectionForeground");
        Color normalFg     = UIManager.getColor("Label.foreground");
        Color mutedFg      = UIManager.getColor("Label.disabledForeground");
        Color hoverBorder  = UIManager.getColor("Component.focusColor");

        JPanel cell = new JPanel(new BorderLayout());
        cell.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (isToday) {
            cell.setBackground(todayBg);
        } else if (isThisMonth) {
            cell.setBackground(thisMonthBg);
        } else {
            cell.setBackground(otherMonthBg);
        }

        JLabel dateLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dateLabel.setFont(dateLabel.getFont().deriveFont(isToday ? Font.BOLD : Font.PLAIN, 13f));
        dateLabel.setForeground(isToday ? todayFg : (isThisMonth ? normalFg : mutedFg));
        cell.add(dateLabel, BorderLayout.NORTH);

        // Show a small dot and a count when this date has assignments due.
        if (due != null && !due.isEmpty()) {
            JLabel marker = new JLabel("\u25CF " + due.size(), SwingConstants.CENTER);
            marker.setForeground(DUE_DOT);
            marker.setFont(marker.getFont().deriveFont(Font.BOLD, 11f));
            cell.add(marker, BorderLayout.CENTER);
            cell.setToolTipText(tooltipFor(due));
        }

        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cell.setBorder(BorderFactory.createLineBorder(hoverBorder, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                cell.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            }
        });

        return cell;
    }

    private static String tooltipFor(List<String> due) {
        // HTML tooltip so each assignment lands on its own line.
        StringBuilder sb = new StringBuilder("<html>");
        for (int i = 0; i < due.size(); i++) {
            if (i > 0) sb.append("<br>");
            sb.append(escapeHtml(due.get(i)));
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

import java.awt.*;
import java.awt.event.*;
import java.time.*;
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
 * highlights any date that has something due.
 */
public class CalendarPanel extends JPanel {

    private static final Color HEADER_BG      = new Color(45, 45, 48);
    private static final Color HEADER_FG      = Color.WHITE;
    private static final Color DAY_NAME_BG    = new Color(60, 60, 65);
    private static final Color DAY_NAME_FG    = new Color(180, 180, 180);
    private static final Color TODAY_BG       = new Color(255, 223, 100);
    private static final Color THIS_MONTH_BG  = Color.WHITE;
    private static final Color OTHER_MONTH_BG = new Color(245, 245, 245);
    private static final Color OTHER_MONTH_FG = new Color(180, 180, 180);
    private static final Color GRID_LINE      = new Color(220, 220, 220);
    private static final Color HOVER_BORDER   = new Color(100, 150, 220);
    private static final Color DUE_DOT        = new Color(220, 70, 70);

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
    }

    /** Rebuilds the grid so newly added/removed assignments show up. */
    public void refresh() {
        rebuildGrid();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton prevButton = makeNavButton("\u25C0"); // left triangle
        JButton nextButton = makeNavButton("\u25B6"); // right triangle

        prevButton.addActionListener(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            rebuildGrid();
        });
        nextButton.addActionListener(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            rebuildGrid();
        });

        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setForeground(HEADER_FG);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        header.add(prevButton,     BorderLayout.WEST);
        header.add(monthYearLabel, BorderLayout.CENTER);
        header.add(nextButton,     BorderLayout.EAST);
        return header;
    }

    private JButton makeNavButton(String symbol) {
        JButton btn = new JButton(symbol);
        btn.setForeground(HEADER_FG);
        btn.setBackground(HEADER_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        return btn;
    }

    private void rebuildGrid() {
        String monthName = displayedMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
        monthYearLabel.setText(monthName + " " + displayedMonth.getYear());

        remove(gridPanel);
        gridPanel = new JPanel(new GridLayout(0, 7, 1, 1));
        gridPanel.setBackground(GRID_LINE);
        gridPanel.setBorder(BorderFactory.createLineBorder(GRID_LINE));

        addDayNameRow();
        addDateCells(collectDueDates());

        add(gridPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void addDayNameRow() {
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String name : dayNames) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(DAY_NAME_BG);
            label.setForeground(DAY_NAME_FG);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
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
            DefaultTableModel tm = subject.getTableModel();
            for (int r = 0; r < tm.getRowCount(); r++) {
                Object dateCell = tm.getValueAt(r, 1);
                Object nameCell = tm.getValueAt(r, 0);
                if (!(dateCell instanceof String)) continue;
                String raw = ((String) dateCell).trim();
                if (raw.isEmpty()) continue;
                try {
                    LocalDate d = LocalDate.parse(raw); // expects yyyy-MM-dd
                    String label = subject.getName() + ": " + nameCell;
                    byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(label);
                } catch (DateTimeParseException ignored) {
                    // Skip malformed dates rather than failing the whole render.
                }
            }
        }
        return byDate;
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
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (isToday) {
            cell.setBackground(TODAY_BG);
        } else if (isThisMonth) {
            cell.setBackground(THIS_MONTH_BG);
        } else {
            cell.setBackground(OTHER_MONTH_BG);
        }

        JLabel dateLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dateLabel.setFont(new Font("SansSerif", isToday ? Font.BOLD : Font.PLAIN, 13));
        dateLabel.setForeground(isThisMonth ? Color.DARK_GRAY : OTHER_MONTH_FG);
        cell.add(dateLabel, BorderLayout.NORTH);

        // Show a small dot and a count when this date has assignments due.
        if (due != null && !due.isEmpty()) {
            JLabel marker = new JLabel("\u25CF " + due.size(), SwingConstants.CENTER);
            marker.setForeground(DUE_DOT);
            marker.setFont(new Font("SansSerif", Font.BOLD, 11));
            cell.add(marker, BorderLayout.CENTER);
            cell.setToolTipText(tooltipFor(due));
        }

        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cell.setBorder(BorderFactory.createLineBorder(HOVER_BORDER, 2));
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

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A small modal calendar popup that lets the user click a date to select it.
 * Returns the chosen date as a LocalDate, or null if the dialog was cancelled.
 *
 * Colors come from the current Look & Feel via UIManager so the popup matches
 * FlatLaf's theme.
 */
public class DatePickerDialog extends JDialog {

    private LocalDate selected;
    private YearMonth displayedMonth;

    private JLabel monthYearLabel;
    private JPanel gridPanel;

    /**
     * Shows the dialog and returns the picked date, or null if cancelled.
     *
     * @param owner   the parent component (used for positioning)
     * @param initial the date to highlight on open, or null for today
     */
    public static LocalDate show(Component owner, LocalDate initial) {
        Window parentWindow = SwingUtilities.getWindowAncestor(owner);
        DatePickerDialog dlg = (parentWindow instanceof Frame)
                ? new DatePickerDialog((Frame) parentWindow, initial)
                : new DatePickerDialog((Dialog) parentWindow, initial);
        dlg.setVisible(true);
        return dlg.selected;
    }

    private DatePickerDialog(Frame owner, LocalDate initial) {
        super(owner, "Pick a Date", true);
        init(initial);
    }

    private DatePickerDialog(Dialog owner, LocalDate initial) {
        super(owner, "Pick a Date", true);
        init(initial);
    }

    private void init(LocalDate initial) {
        selected = null;
        displayedMonth = YearMonth.from(initial != null ? initial : LocalDate.now());

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        gridPanel = new JPanel();
        add(gridPanel, BorderLayout.CENTER);
        rebuildGrid(initial);

        setResizable(false);
        pack();
        setLocationRelativeTo(getOwner());
    }

    // -----------------------------------------------------------------------
    // Header (month/year + prev/next)
    // -----------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JButton prev = makeNavBtn("\u25C0");
        JButton next = makeNavBtn("\u25B6");

        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(monthYearLabel.getFont().deriveFont(Font.BOLD, 14f));

        prev.addActionListener(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            rebuildGrid(null);
        });
        next.addActionListener(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            rebuildGrid(null);
        });

        header.add(prev,            BorderLayout.WEST);
        header.add(monthYearLabel,  BorderLayout.CENTER);
        header.add(next,            BorderLayout.EAST);
        return header;
    }

    private JButton makeNavBtn(String symbol) {
        JButton btn = new JButton(symbol);
        btn.putClientProperty("JButton.buttonType", "toolBarButton");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        return btn;
    }

    // -----------------------------------------------------------------------
    // Grid
    // -----------------------------------------------------------------------

    private void rebuildGrid(LocalDate highlighted) {
        String monthName = displayedMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
        monthYearLabel.setText(monthName + " " + displayedMonth.getYear());

        Color gridLine   = UIManager.getColor("Component.borderColor");
        Color dayNameBg  = UIManager.getColor("TableHeader.background");
        Color dayNameFg  = UIManager.getColor("TableHeader.foreground");

        remove(gridPanel);
        gridPanel = new JPanel(new GridLayout(0, 7, 1, 1));
        gridPanel.setBackground(gridLine);
        gridPanel.setBorder(new LineBorder(gridLine));

        // Day-name row
        String[] dayNames = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (String n : dayNames) {
            JLabel lbl = new JLabel(n, SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(dayNameBg);
            lbl.setForeground(dayNameFg);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            gridPanel.add(lbl);
        }

        LocalDate today        = LocalDate.now();
        LocalDate firstOfMonth = displayedMonth.atDay(1);
        int startOffset        = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate cellDate     = firstOfMonth.minusDays(startOffset);

        for (int i = 0; i < 42; i++) {
            gridPanel.add(buildCell(cellDate, today, highlighted));
            cellDate = cellDate.plusDays(1);
        }

        add(gridPanel, BorderLayout.CENTER);
        revalidate();
        pack();
    }

    private JPanel buildCell(LocalDate date, LocalDate today, LocalDate highlighted) {
        Color thisMonthBg  = UIManager.getColor("Table.background");
        Color otherMonthBg = UIManager.getColor("Panel.background");
        Color todayBg      = UIManager.getColor("Table.selectionBackground");
        Color todayFg      = UIManager.getColor("Table.selectionForeground");
        Color normalFg     = UIManager.getColor("Label.foreground");
        Color mutedFg      = UIManager.getColor("Label.disabledForeground");
        Color hoverBorder  = UIManager.getColor("Component.focusColor");

        boolean isThisMonth   = YearMonth.from(date).equals(displayedMonth);
        boolean isToday       = date.equals(today);
        boolean isHighlighted = date.equals(highlighted);

        JPanel cell = new JPanel(new BorderLayout());
        cell.setPreferredSize(new Dimension(36, 30));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color bg;
        if (isHighlighted || isToday) {
            bg = todayBg;
        } else if (isThisMonth) {
            bg = thisMonthBg;
        } else {
            bg = otherMonthBg;
        }
        cell.setBackground(bg);
        cell.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

        JLabel lbl = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(isToday ? Font.BOLD : Font.PLAIN, 12f));
        Color fg = (isHighlighted || isToday) ? todayFg
                 : isThisMonth                ? normalFg
                 :                              mutedFg;
        lbl.setForeground(fg);
        cell.add(lbl, BorderLayout.CENTER);

        final LocalDate picked = date;
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selected = picked;
                dispose();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                cell.setBorder(new LineBorder(hoverBorder, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                cell.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            }
        });

        return cell;
    }
}

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class NotificationService {

    private static final String PREF_ENABLED      = "notif_enabled";
    private static final String PREF_DAYS_AHEAD   = "notif_days_ahead";
    private static final String PREF_SHOW_OVERDUE = "notif_show_overdue";
    private static final String PREF_HOUR         = "notif_hour";

    private static final Preferences PREFS = Preferences.userNodeForPackage(NotificationService.class);

    private final DefaultListModel<Subject> subjectModel;
    private final JFrame parentFrame;
    private Timer dailyTimer;

    public NotificationService(DefaultListModel<Subject> subjectModel, JFrame parentFrame) {
        this.subjectModel = subjectModel;
        this.parentFrame  = parentFrame;
    }

    public void start() {
        checkAndNotify();
        scheduleDailyTimer();
    }

    private void scheduleDailyTimer() {
        if (dailyTimer != null) dailyTimer.stop();
        int targetHour = PREFS.getInt(PREF_HOUR, 8);
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, targetHour);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= now) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        int delay = (int) Math.min(cal.getTimeInMillis() - now, Integer.MAX_VALUE);
        dailyTimer = new Timer(delay, e -> {
            checkAndNotify();
            dailyTimer.setDelay(24 * 60 * 60 * 1000);
            dailyTimer.restart();
        });
        dailyTimer.setRepeats(false);
        dailyTimer.start();
    }

    public void checkAndNotify() {
        if (!PREFS.getBoolean(PREF_ENABLED, true)) return;

        int daysAhead    = PREFS.getInt(PREF_DAYS_AHEAD, 3);
        boolean overdue  = PREFS.getBoolean(PREF_SHOW_OVERDUE, true);
        LocalDate today  = LocalDate.now();

        List<String> upcoming = new ArrayList<>();
        List<String> overdueList = new ArrayList<>();

        for (int i = 0; i < subjectModel.size(); i++) {
            Subject subject = subjectModel.get(i);
            DefaultTableModel tm = subject.getTableModel();
            for (int r = 0; r < tm.getRowCount(); r++) {
                Boolean done = (Boolean) tm.getValueAt(r, 2);
                if (Boolean.TRUE.equals(done)) continue;
                String raw = (String) tm.getValueAt(r, 1);
                if (raw == null || raw.trim().isEmpty()) continue;
                LocalDate due = parseDate(raw.trim());
                if (due == null) continue;
                String label = subject.getName() + ": " + tm.getValueAt(r, 0)
                        + "  (due " + raw.trim() + ")";
                if (due.isBefore(today)) {
                    if (overdue) overdueList.add(label);
                } else if (!due.isAfter(today.plusDays(daysAhead))) {
                    upcoming.add(label);
                }
            }
        }

        if (upcoming.isEmpty() && overdueList.isEmpty()) return;

        SwingUtilities.invokeLater(() -> showNotificationDialog(upcoming, overdueList));
    }

    private void showNotificationDialog(List<String> upcoming, List<String> overdueList) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        if (!overdueList.isEmpty()) {
            JLabel overdueHeader = new JLabel("Overdue:");
            overdueHeader.setFont(overdueHeader.getFont().deriveFont(Font.BOLD, 13f));
            overdueHeader.setForeground(new Color(220, 70, 70));
            content.add(overdueHeader);
            content.add(Box.createVerticalStrut(4));
            for (String s : overdueList) {
                JLabel lbl = new JLabel("  \u2022 " + s);
                lbl.setForeground(new Color(220, 70, 70));
                content.add(lbl);
            }
            content.add(Box.createVerticalStrut(10));
        }

        if (!upcoming.isEmpty()) {
            JLabel upcomingHeader = new JLabel("Upcoming:");
            upcomingHeader.setFont(upcomingHeader.getFont().deriveFont(Font.BOLD, 13f));
            content.add(upcomingHeader);
            content.add(Box.createVerticalStrut(4));
            for (String s : upcoming) {
                content.add(new JLabel("  \u2022 " + s));
            }
        }

        panel.add(new JScrollPane(content), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(parentFrame, panel,
                "Assignment Reminder", JOptionPane.WARNING_MESSAGE);
    }

    public void showSettingsDialog() {
        JDialog dialog = new JDialog(parentFrame, "Notification Settings", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 6, 6, 6);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        JCheckBox enabledBox = new JCheckBox("Enable notifications",
                PREFS.getBoolean(PREF_ENABLED, true));

        JCheckBox overdueBox = new JCheckBox("Include overdue assignments",
                PREFS.getBoolean(PREF_SHOW_OVERDUE, true));

        SpinnerNumberModel daysModel = new SpinnerNumberModel(
                PREFS.getInt(PREF_DAYS_AHEAD, 3), 0, 30, 1);
        JSpinner daysSpinner = new JSpinner(daysModel);
        ((JSpinner.DefaultEditor) daysSpinner.getEditor()).getTextField().setColumns(3);

        SpinnerNumberModel hourModel = new SpinnerNumberModel(
                PREFS.getInt(PREF_HOUR, 8), 0, 23, 1);
        JSpinner hourSpinner = new JSpinner(hourModel);
        ((JSpinner.DefaultEditor) hourSpinner.getEditor()).getTextField().setColumns(3);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        form.add(enabledBox, gbc);

        gbc.gridy = 1;
        form.add(overdueBox, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2; gbc.gridx = 0;
        form.add(new JLabel("Notify days before due:"), gbc);
        gbc.gridx = 1;
        form.add(daysSpinner, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        form.add(new JLabel("Daily reminder hour (0-23):"), gbc);
        gbc.gridx = 1;
        form.add(hourSpinner, gbc);

        dialog.add(form, BorderLayout.CENTER);

        JButton saveBtn   = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            PREFS.putBoolean(PREF_ENABLED,      enabledBox.isSelected());
            PREFS.putBoolean(PREF_SHOW_OVERDUE, overdueBox.isSelected());
            PREFS.putInt(PREF_DAYS_AHEAD,       (int) daysSpinner.getValue());
            PREFS.putInt(PREF_HOUR,             (int) hourSpinner.getValue());
            scheduleDailyTimer();
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ignored) { }
        return null;
    }
}

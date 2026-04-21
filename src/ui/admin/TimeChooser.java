package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Modern Time Picker component featuring a "Clock-like" interface.
 * Consists of a read-only text field and a clock icon button.
 */
public class TimeChooser extends JPanel {
    private JTextField timeField;
    private JButton pickerButton;
    private Date selectedTime;
    private SimpleDateFormat displayFmt = new SimpleDateFormat("HH:mm");
    private JPopupMenu timeMenu;

    private static final Color ACCENT = new Color(74, 144, 226);
    private static final Color ACCENT_LIGHT = new Color(74, 144, 226, 40);
    private static final Color TEXT_PRI = new Color(26, 35, 64);
    private static final Color BG_LIGHT = new Color(248, 250, 253);

    public TimeChooser() {
        this(new Date());
    }

    public TimeChooser(Date initialTime) {
        setLayout(new BorderLayout(5, 0));
        setOpaque(false);
        this.selectedTime = initialTime != null ? initialTime : new Date();

        timeField = new JTextField(displayFmt.format(selectedTime));
        timeField.setEditable(false);
        timeField.setBackground(Color.WHITE);
        timeField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        timeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        pickerButton = new JButton("🕒");
        pickerButton.setFocusPainted(false);
        pickerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pickerButton.setBackground(ACCENT);
        pickerButton.setForeground(Color.WHITE);
        pickerButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        pickerButton.addActionListener(e -> showTimePopup());

        add(timeField, BorderLayout.CENTER);
        add(pickerButton, BorderLayout.EAST);
    }

    public void setTime(Date time) {
        if (time == null) return;
        this.selectedTime = time;
        timeField.setText(displayFmt.format(selectedTime));
    }

    public void setTime(String hhmm) {
        try {
            this.selectedTime = displayFmt.parse(hhmm);
            timeField.setText(hhmm);
        } catch (Exception e) {}
    }

    public Date getTime() {
        return selectedTime;
    }

    public String getTimeString() {
        return displayFmt.format(selectedTime);
    }

    private void showTimePopup() {
        timeMenu = new JPopupMenu();
        timeMenu.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240)));
        timeMenu.add(new TimePanel());
        timeMenu.show(pickerButton, -210, pickerButton.getHeight());
    }

    private class TimePanel extends JPanel {
        private int hour, minute;
        private boolean isAM;
        private JLabel timeLabel;

        public TimePanel() {
            Calendar c = Calendar.getInstance();
            c.setTime(selectedTime);
            hour = c.get(Calendar.HOUR);
            if (hour == 0) hour = 12;
            minute = c.get(Calendar.MINUTE);
            isAM = c.get(Calendar.AM_PM) == Calendar.AM;

            setLayout(new BorderLayout(0, 10));
            setPreferredSize(new Dimension(250, 320));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(15, 15, 15, 15));

            // Header Display
            timeLabel = new JLabel("", SwingConstants.CENTER);
            timeLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            timeLabel.setForeground(ACCENT);
            updateLabel();
            add(timeLabel, BorderLayout.NORTH);

            // Selection area: Tabs for Hour / Minute
            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(new Font("SansSerif", Font.BOLD, 11));
            tabs.addTab("HOURS", createHourGrid());
            tabs.addTab("MINS", createMinuteGrid());
            add(tabs, BorderLayout.CENTER);

            // Footer: AM/PM and OK
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);

            JPanel ampmToggle = new JPanel(new GridLayout(1, 2, 5, 0));
            ampmToggle.setOpaque(false);
            JButton amBtn = createToggleButton("AM", isAM);
            JButton pmBtn = createToggleButton("PM", !isAM);
            amBtn.addActionListener(e -> { isAM = true; amBtn.setBackground(ACCENT); amBtn.setForeground(Color.WHITE); pmBtn.setBackground(BG_LIGHT); pmBtn.setForeground(TEXT_PRI); updateLabel(); });
            pmBtn.addActionListener(e -> { isAM = false; pmBtn.setBackground(ACCENT); pmBtn.setForeground(Color.WHITE); amBtn.setBackground(BG_LIGHT); amBtn.setForeground(TEXT_PRI); updateLabel(); });
            ampmToggle.add(amBtn); ampmToggle.add(pmBtn);

            JButton okBtn = new JButton("OK");
            okBtn.setBackground(ACCENT);
            okBtn.setForeground(Color.WHITE);
            okBtn.setFocusPainted(false);
            okBtn.addActionListener(e -> confirmTime());

            footer.add(ampmToggle, BorderLayout.WEST);
            footer.add(okBtn, BorderLayout.EAST);
            add(footer, BorderLayout.SOUTH);
        }

        private JPanel createHourGrid() {
            JPanel p = new JPanel(new GridLayout(3, 4, 5, 5));
            p.setOpaque(false);
            for (int i = 1; i <= 12; i++) {
                final int h = i;
                JButton b = createGridButton(String.valueOf(h), h == hour);
                b.addActionListener(e -> { hour = h; updateLabel(); p.repaint(); });
                p.add(b);
            }
            return p;
        }

        private JPanel createMinuteGrid() {
            JPanel p = new JPanel(new GridLayout(3, 4, 5, 5));
            p.setOpaque(false);
            int[] mins = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
            for (int m : mins) {
                final int min = m;
                JButton b = createGridButton(String.format("%02d", min), min == minute);
                b.addActionListener(e -> { minute = min; updateLabel(); p.repaint(); });
                p.add(b);
            }
            return p;
        }

        private JButton createGridButton(String txt, boolean selected) {
            JButton b = new JButton(txt);
            b.setFont(new Font("SansSerif", Font.PLAIN, 12));
            b.setFocusPainted(false);
            b.setBackground(selected ? ACCENT : BG_LIGHT);
            b.setForeground(selected ? Color.WHITE : TEXT_PRI);
            b.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return b;
        }

        private JButton createToggleButton(String txt, boolean active) {
            JButton b = new JButton(txt);
            b.setFont(new Font("SansSerif", Font.BOLD, 10));
            b.setFocusPainted(false);
            b.setBackground(active ? ACCENT : BG_LIGHT);
            b.setForeground(active ? Color.WHITE : TEXT_PRI);
            b.setPreferredSize(new Dimension(45, 28));
            return b;
        }

        private void updateLabel() {
            timeLabel.setText(String.format("%02d:%02d %s", hour, minute, isAM ? "AM" : "PM"));
        }

        private void confirmTime() {
            Calendar c = Calendar.getInstance();
            int h24 = hour;
            if (isAM && h24 == 12) h24 = 0;
            if (!isAM && h24 != 12) h24 += 12;
            c.set(Calendar.HOUR_OF_DAY, h24);
            c.set(Calendar.MINUTE, minute);
            selectedTime = c.getTime();
            timeField.setText(displayFmt.format(selectedTime));
            timeMenu.setVisible(false);
        }
    }
}

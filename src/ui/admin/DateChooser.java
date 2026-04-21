package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Custom DateChooser component that provides a polished calendar display.
 * It consists of a read-only text field and a calendar icon button.
 */
public class DateChooser extends JPanel {
    private JTextField dateField;
    private JButton pickerButton;
    private Date selectedDate;
    private SimpleDateFormat displayFmt = new SimpleDateFormat("yyyy-MM-dd");
    private JPopupMenu calendarMenu;

    private static final Color ACCENT = new Color(74, 144, 226);
    private static final Color TEXT_PRI = new Color(26, 35, 64);

    public DateChooser() {
        this(new Date());
    }

    public DateChooser(Date initialDate) {
        setLayout(new BorderLayout(5, 0));
        setOpaque(false);
        this.selectedDate = initialDate;

        dateField = new JTextField(displayFmt.format(selectedDate));
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        pickerButton = new JButton("📅");
        pickerButton.setFocusPainted(false);
        pickerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pickerButton.setBackground(ACCENT);
        pickerButton.setForeground(Color.WHITE);
        pickerButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        pickerButton.addActionListener(e -> showCalendar());

        add(dateField, BorderLayout.CENTER);
        add(pickerButton, BorderLayout.EAST);
    }

    public void setDate(Date date) {
        if (date == null) return;
        this.selectedDate = date;
        dateField.setText(displayFmt.format(selectedDate));
    }

    public Date getDate() {
        return selectedDate;
    }

    private void showCalendar() {
        calendarMenu = new JPopupMenu();
        calendarMenu.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240)));
        calendarMenu.add(new CalendarPanel());
        calendarMenu.show(pickerButton, -180, pickerButton.getHeight());
    }

    private class CalendarPanel extends JPanel {
        private Calendar cal;
        private JLabel monthLabel;
        private JPanel daysPanel;

        public CalendarPanel() {
            cal = Calendar.getInstance();
            cal.setTime(selectedDate);
            setLayout(new BorderLayout(0, 5));
            setPreferredSize(new Dimension(220, 220));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(10, 10, 10, 10));

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            
            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            monthLabel.setForeground(TEXT_PRI);
            
            JButton prev = createNavButton("<");
            prev.addActionListener(e -> { cal.add(Calendar.MONTH, -1); updateCalendar(); });
            
            JButton next = createNavButton(">");
            next.addActionListener(e -> { cal.add(Calendar.MONTH, 1); updateCalendar(); });
            
            header.add(prev, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);
            
            add(header, BorderLayout.NORTH);

            // Days panel
            daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));
            daysPanel.setOpaque(false);
            add(daysPanel, BorderLayout.CENTER);

            updateCalendar();
        }

        private JButton createNavButton(String txt) {
            JButton b = new JButton(txt);
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setFont(new Font("Monospaced", Font.BOLD, 14));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return b;
        }

        private void updateCalendar() {
            daysPanel.removeAll();
            SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy");
            monthLabel.setText(monthFmt.format(cal.getTime()));

            String[] headers = {"S", "M", "T", "W", "T", "F", "S"};
            for (String h : headers) {
                JLabel l = new JLabel(h, SwingConstants.CENTER);
                l.setFont(new Font("SansSerif", Font.BOLD, 11));
                l.setForeground(new Color(150, 160, 180));
                daysPanel.add(l);
            }

            Calendar temp = (Calendar) cal.clone();
            temp.set(Calendar.DAY_OF_MONTH, 1);
            int startDay = temp.get(Calendar.DAY_OF_WEEK) - 1;
            int maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < startDay; i++) daysPanel.add(new JLabel(""));

            for (int i = 1; i <= maxDays; i++) {
                final int day = i;
                JButton dayBtn = new JButton(String.valueOf(i));
                dayBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
                dayBtn.setFocusPainted(false);
                dayBtn.setBackground(Color.WHITE);
                dayBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                dayBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // Highlight selected date
                Calendar sel = Calendar.getInstance();
                sel.setTime(selectedDate);
                if (temp.get(Calendar.YEAR) == sel.get(Calendar.YEAR) &&
                    temp.get(Calendar.MONTH) == sel.get(Calendar.MONTH) &&
                    i == sel.get(Calendar.DAY_OF_MONTH)) {
                    dayBtn.setBackground(ACCENT);
                    dayBtn.setForeground(Color.WHITE);
                } else {
                    dayBtn.setForeground(TEXT_PRI);
                }

                dayBtn.addActionListener(e -> {
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    selectedDate = cal.getTime();
                    dateField.setText(displayFmt.format(selectedDate));
                    calendarMenu.setVisible(false);
                });

                daysPanel.add(dayBtn);
            }
            daysPanel.revalidate();
            daysPanel.repaint();
        }
    }
}

package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Modern Time Picker component using JSpinner for robust time selection.
 * Supports AM/PM selection and manual editing.
 */
public class TimeChooser extends JPanel {
    private JSpinner timeSpinner;
    private SimpleDateFormat displayFmt = new SimpleDateFormat("hh:mm a");
    
    private static final Color TEXT_PRI = new Color(26, 35, 64);

    public TimeChooser() {
        this(new Date());
    }

    public TimeChooser(Date initialTime) {
        setLayout(new BorderLayout());
        setOpaque(false);

        SpinnerDateModel model = new SpinnerDateModel();
        timeSpinner = new JSpinner(model);
        
        JSpinner.DateEditor editor = new JSpinner.DateEditor(timeSpinner, "hh:mm a");
        timeSpinner.setEditor(editor);
        
        // Style the spinner
        timeSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        timeSpinner.setForeground(TEXT_PRI);
        timeSpinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(2, 5, 2, 5)));

        if (initialTime != null) {
            timeSpinner.setValue(initialTime);
        }

        add(timeSpinner, BorderLayout.CENTER);
    }

    public void setTime(Date time) {
        if (time == null) return;
        timeSpinner.setValue(time);
    }

    public void setTime(String hhmm) {
        try {
            // Support both 24h and AM/PM formats if possible, or just default to 24h for simple string setting
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            Date date = parser.parse(hhmm);
            timeSpinner.setValue(date);
        } catch (Exception e) {
            try {
                timeSpinner.setValue(displayFmt.parse(hhmm));
            } catch (Exception ex) {}
        }
    }

    public Date getTime() {
        return (Date) timeSpinner.getValue();
    }

    public String getTimeString() {
        return displayFmt.format(getTime());
    }
}


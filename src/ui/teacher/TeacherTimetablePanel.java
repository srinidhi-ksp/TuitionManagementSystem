package ui.teacher;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import model.Batch;
import model.Schedule;
import service.TeacherPortalService;
import util.SessionManager;
import util.ThemeManager;

public class TeacherTimetablePanel extends JPanel {

    private final String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private JPanel gridContainer;
    private List<CellEntry> entries = new ArrayList<>();

    public TeacherTimetablePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("TEACHER Weekly Timetable");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ThemeManager.TEXT);
        add(title, BorderLayout.NORTH);

        gridContainer = new JPanel();
        gridContainer.setBackground(ThemeManager.BG);

        JScrollPane scroll = new JScrollPane(gridContainer);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        refreshTimetable();
    }

    private void refreshTimetable() {
        gridContainer.removeAll();
        gridContainer.add(new JLabel("Loading timetable..."));

        new SwingWorker<List<Batch>, Void>() {
            @Override protected List<Batch> doInBackground() {
                return new TeacherPortalService().getTeacherBatches(SessionManager.getCurrentTeacherId());
            }

            @Override protected void done() {
                try {
                    buildGrid(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TeacherTimetablePanel.this, "Failed to load timetable: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void buildGrid(List<Batch> batches) {
        entries.clear();
        TreeSet<String> timeSlots = new TreeSet<>();
        if (batches != null) {
            for (Batch batch : batches) {
                if (batch.getSchedules() == null) continue;
                for (Schedule schedule : batch.getSchedules()) {
                    if (schedule.getDay() == null || schedule.getStart() == null || schedule.getEnd() == null) continue;
                    String slot = schedule.getStart() + " - " + schedule.getEnd();
                    timeSlots.add(slot);
                    entries.add(new CellEntry(batch, schedule, slot));
                }
            }
        }
        if (timeSlots.isEmpty()) {
            timeSlots.add("09:00 - 10:30");
            timeSlots.add("11:00 - 12:30");
            timeSlots.add("14:00 - 15:30");
        }

        gridContainer.removeAll();
        gridContainer.setLayout(new GridLayout(8, timeSlots.size() + 1, 10, 10));
        gridContainer.add(createHeaderLabel("DAY / TIME"));
        for (String slot : timeSlots) gridContainer.add(createHeaderLabel(slot));

        Map<String, Map<String, List<CellEntry>>> timetable = createTimetable(timeSlots);
        for (String day : days) {
            gridContainer.add(createDayLabel(day));
            for (String slot : timeSlots) {
                gridContainer.add(createCell(day, slot, timetable.get(day).get(slot)));
            }
        }
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private Map<String, Map<String, List<CellEntry>>> createTimetable(TreeSet<String> timeSlots) {
        Map<String, Map<String, List<CellEntry>>> table = new TreeMap<>();
        for (String day : days) {
            Map<String, List<CellEntry>> row = new TreeMap<>();
            for (String slot : timeSlots) row.put(slot, new ArrayList<>());
            table.put(day, row);
        }
        for (CellEntry entry : entries) {
            for (String slot : timeSlots) {
                if (entry.schedule.getDay() != null
                        && table.containsKey(entry.schedule.getDay().toUpperCase())
                        && overlaps(slot, entry.schedule)) {
                    table.get(entry.schedule.getDay().toUpperCase()).get(slot).add(entry);
                }
            }
        }
        return table;
    }

    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(ThemeManager.SUB_TEXT);
        label.setOpaque(true);
        label.setBackground(ThemeManager.BG);
        label.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return label;
    }

    private JLabel createDayLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(ThemeManager.TEXT);
        label.setOpaque(true);
        label.setBackground(new Color(241, 245, 249));
        label.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return label;
    }

    private JPanel createCell(String day, String slot, List<CellEntry> matches) {
        JPanel cell = new JPanel(new GridLayout(0, 1, 2, 2));
        cell.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        cell.setBackground(ThemeManager.CARD);

        if (matches == null || matches.isEmpty()) {
            JLabel dash = new JLabel("-", SwingConstants.CENTER);
            dash.setForeground(Color.LIGHT_GRAY);
            cell.add(dash);
        } else if (matches.size() == 1) {
            CellEntry entry = matches.get(0);
            cell.setBackground(new Color(219, 234, 254));
            JLabel name = new JLabel("<html><center><b>" + entry.batch.getBatchName() + "</b></center></html>", SwingConstants.CENTER);
            name.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cell.add(name);
            cell.setToolTipText("Teacher: " + SessionManager.getInstance().getUserName());
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) { showBatchDetails(entry); }
            });
        } else {
            cell.setBackground(new Color(254, 226, 226));
            JLabel conflict = new JLabel("<html><center><b>CONFLICT</b><br>" + matches.size() + " Batches</center></html>", SwingConstants.CENTER);
            conflict.setForeground(new Color(220, 38, 38));
            conflict.setFont(new Font("SansSerif", Font.BOLD, 10));
            cell.add(conflict);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.setToolTipText("Conflict with " + matches.size() + " batches");
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) { showConflictDetails(day, slot, matches); }
            });
        }
        return cell;
    }

    private boolean overlaps(String slot, Schedule schedule) {
        String[] parts = slot.split(" - ");
        if (parts.length != 2) return false;
        int slotStart = minutes(parts[0]);
        int slotEnd = minutes(parts[1]);
        int start = minutes(schedule.getStart());
        int end = minutes(schedule.getEnd());
        return slotStart >= 0 && slotEnd >= 0 && start < slotEnd && end > slotStart;
    }

    private int minutes(String value) {
        try {
            String[] parts = value.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private void showBatchDetails(CellEntry entry) {
        String msg = "Batch Details\n\n"
                + "Batch Name: " + entry.batch.getBatchName() + "\n"
                + "Teacher: " + SessionManager.getInstance().getUserName() + "\n"
                + "Schedule: " + entry.schedule.getDay() + " " + entry.schedule.getStart() + " - " + entry.schedule.getEnd() + "\n"
                + "Mode: " + (entry.batch.getClassMode() != null ? entry.batch.getClassMode() : "-");
        JOptionPane.showMessageDialog(this, msg, "Batch Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showConflictDetails(String day, String slot, List<CellEntry> matches) {
        StringBuilder msg = new StringBuilder("Conflict Detected\n\n");
        msg.append("Time Slot: ").append(slot).append("\n");
        msg.append("Day: ").append(day).append("\n\n");
        int i = 1;
        for (CellEntry entry : matches) {
            msg.append("Batch ").append(i++).append(": ").append(entry.batch.getBatchName()).append("\n");
            msg.append("Teacher: ").append(SessionManager.getInstance().getUserName()).append("\n\n");
        }
        JOptionPane.showMessageDialog(this, msg.toString(), "Conflict Detected", JOptionPane.WARNING_MESSAGE);
    }

    private static class CellEntry {
        Batch batch;
        Schedule schedule;
        String slot;
        CellEntry(Batch batch, Schedule schedule, String slot) {
            this.batch = batch;
            this.schedule = schedule;
            this.slot = slot;
        }
    }
}

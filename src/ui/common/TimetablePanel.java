package ui.common;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.BatchDAO;
import model.Batch;
import model.Schedule;
import util.ThemeManager;

public class TimetablePanel extends JPanel {

    private String viewType; // ADMIN, TEACHER, STUDENT
    private String filterId; // teacherId or studentId
    private BatchDAO batchDAO;
    
    private JPanel gridContainer;
    private final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private Set<String> timeSlots = new TreeSet<>();

    public TimetablePanel(String viewType, String filterId) {
        this.viewType = viewType;
        this.filterId = filterId;
        this.batchDAO = new BatchDAO();

        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel(viewType + " Weekly Timetable");
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

    public void refreshTimetable() {
        List<Batch> batches;
        if ("TEACHER".equals(viewType)) {
            batches = batchDAO.getBatchesByTeacher(filterId);
        } else if ("STUDENT".equals(viewType)) {
            batches = batchDAO.getBatchesByStudentEnrollment(filterId);
        } else {
            batches = batchDAO.getAllBatches();
        }

        // 1. Extract dynamic time slots
        timeSlots.clear();
        for (Batch b : batches) {
            if (b.getSchedules() != null) {
                for (Schedule s : b.getSchedules()) {
                    timeSlots.add(s.getStart() + " - " + s.getEnd());
                }
            }
        }

        if (timeSlots.isEmpty()) {
            timeSlots.add("09:00 - 10:30");
            timeSlots.add("11:00 - 12:30");
            timeSlots.add("14:00 - 15:30");
        }

        // 2. Build Grid
        gridContainer.removeAll();
        int cols = timeSlots.size() + 1; // +1 for Days column
        gridContainer.setLayout(new GridLayout(8, cols, 10, 10)); // 7 days + 1 header row

        // Header Row
        gridContainer.add(createHeaderLabel("DAY / TIME"));
        for (String slot : timeSlots) {
            gridContainer.add(createHeaderLabel(slot));
        }

        // Day Rows
        for (String day : DAYS) {
            gridContainer.add(createDayLabel(day));
            for (String slot : timeSlots) {
                gridContainer.add(createCell(day, slot, batches));
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private JLabel createHeaderLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(ThemeManager.SUB_TEXT);
        l.setOpaque(true);
        l.setBackground(ThemeManager.BG);
        l.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return l;
    }

    private JLabel createDayLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(ThemeManager.TEXT);
        l.setOpaque(true);
        l.setBackground(new Color(241, 245, 249));
        l.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return l;
    }

    private JPanel createCell(String day, String slot, List<Batch> batches) {
        JPanel cell = new JPanel(new GridLayout(0, 1, 2, 2));
        cell.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        cell.setBackground(ThemeManager.CARD);

        List<Batch> matches = new ArrayList<>();
        for (Batch b : batches) {
            if (b.getSchedules() != null) {
                for (Schedule s : b.getSchedules()) {
                    String bSlot = s.getStart() + " - " + s.getEnd();
                    if (day.equals(s.getDay()) && slot.equals(bSlot)) {
                        matches.add(b);
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            JLabel dash = new JLabel("-", SwingConstants.CENTER);
            dash.setForeground(Color.LIGHT_GRAY);
            cell.add(dash);
        } else if (matches.size() == 1) {
            Batch b = matches.get(0);
            Color defaultBg = new Color(219, 234, 254);
            Color hoverBg = new Color(191, 219, 254);
            cell.setBackground(defaultBg);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            dao.TeacherDAO tDAO = new dao.TeacherDAO();
            model.Teacher teacher = tDAO.getTeacherById(b.getTeacherId());
            String tName = teacher != null ? teacher.getName() : "Not Assigned";
            
            cell.setToolTipText("Teacher: " + tName);
            
            JLabel name = new JLabel("<html><div style='text-align:center;'><b>" + b.getBatchName() + "</b></div></html>", SwingConstants.CENTER);
            name.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cell.add(name);
            
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { cell.setBackground(hoverBg); }
                public void mouseExited(java.awt.event.MouseEvent e) { cell.setBackground(defaultBg); }
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showBatchDetailsPopup(b, tName, day, slot);
                }
            });
        } else {
            Color defaultBg = new Color(254, 226, 226);
            Color hoverBg = new Color(254, 202, 202);
            cell.setBackground(defaultBg);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.setToolTipText("Conflict Detected! Click for details.");
            
            JLabel conf = new JLabel("<html><div style='text-align:center;'><b>⚠ CONFLICT</b><br>" + matches.size() + " Batches</div></html>", SwingConstants.CENTER);
            conf.setForeground(new Color(220, 38, 38));
            conf.setFont(new Font("SansSerif", Font.BOLD, 10));
            cell.add(conf);
            
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { cell.setBackground(hoverBg); }
                public void mouseExited(java.awt.event.MouseEvent e) { cell.setBackground(defaultBg); }
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showConflictPopup(matches, day, slot);
                }
            });
        }

        return cell;
    }

    private void showBatchDetailsPopup(Batch b, String tName, String day, String slot) {
        dao.EnrollmentDAO eDAO = new dao.EnrollmentDAO();
        long studentCount = eDAO.getEnrollmentsByBatchId(b.getBatchId()).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())).count();
                
        String details = "<html><body style='width: 250px; padding: 10px; font-family: SansSerif;'>"
            + "<h2 style='color:#1e3a8a; margin-top:0;'>Batch Details</h2>"
            + "<b>Batch Name:</b> " + b.getBatchName() + "<br><br>"
            + "<b>Teacher:</b> " + tName + "<br><br>"
            + "<b>Total Students:</b> " + studentCount + "<br><br>"
            + "<b>Schedule:</b> " + day + " " + slot + "<br><br>"
            + "<b>Mode:</b> " + (b.getMode() != null ? b.getMode() : "Offline")
            + "</body></html>";
            
        JOptionPane.showMessageDialog(this, new JLabel(details), "Batch Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showConflictPopup(List<Batch> matches, String day, String slot) {
        dao.TeacherDAO tDAO = new dao.TeacherDAO();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width: 300px; padding: 10px; font-family: SansSerif;'>");
        sb.append("<h2 style='color:#dc2626; margin-top:0;'>⚠ Schedule Conflict Detected</h2>");
        sb.append("<b>Time Slot:</b> ").append(slot).append("<br>");
        sb.append("<b>Day:</b> ").append(day).append("<br><br>");
        sb.append("<b>Conflicting Batches:</b><br><br>");
        
        for (int i = 0; i < matches.size(); i++) {
            Batch b = matches.get(i);
            model.Teacher t = tDAO.getTeacherById(b.getTeacherId());
            String tName = t != null ? t.getName() : "Not Assigned";
            sb.append(i + 1).append(". <b>Batch:</b> ").append(b.getBatchName()).append("<br>");
            sb.append("&nbsp;&nbsp;&nbsp;<b>Teacher:</b> ").append(tName).append("<br><br>");
        }
        sb.append("</body></html>");
        
        JOptionPane.showMessageDialog(this, new JLabel(sb.toString()), "Conflict Details", JOptionPane.WARNING_MESSAGE);
    }
}

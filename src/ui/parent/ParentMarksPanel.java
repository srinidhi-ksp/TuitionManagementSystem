package ui.parent;

import java.awt.*;
import java.io.File;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.Student;
import model.TestMark;
import service.PDFReportService;
import service.ParentPortalService;
import util.SessionManager;
import util.ThemeManager;

/**
 * Parent Marks Panel - Detailed test performance and Report Card download
 */
public class ParentMarksPanel extends JPanel {

    private ParentPortalService portalService;
    private PDFReportService pdfService;
    private JComboBox<String> studentSelector;
    private JTable marksTable;
    private DefaultTableModel model;
    private List<Student> linkedStudents;
    private Student currentStudent;

    private JPanel chartPanel;
    private List<TestMark> currentMarks;

    public ParentMarksPanel() {
        this.portalService = new ParentPortalService();
        this.pdfService = new PDFReportService();
        setLayout(new BorderLayout(0, 24));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initContent();
        
        loadInitialData();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("Academic Performance");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("Visual performance analytics and detailed report cards");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titles.add(title);
        titles.add(sub);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actions.setBackground(ThemeManager.BG);

        studentSelector = new JComboBox<>();
        studentSelector.setPreferredSize(new Dimension(200, 38));
        studentSelector.addActionListener(e -> onStudentSelected());

        JButton downloadBtn = new JButton("📥 Download Report Card");
        downloadBtn.setPreferredSize(new Dimension(200, 38));
        downloadBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        downloadBtn.setBackground(new Color(59, 130, 246));
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.setFocusPainted(false);
        downloadBtn.addActionListener(e -> downloadReport());

        actions.add(studentSelector);
        actions.add(downloadBtn);

        header.add(titles, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel center = new JPanel(new BorderLayout(0, 30));
        center.setBackground(ThemeManager.BG);

        // Top: Bar Chart Area
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart((Graphics2D) g);
            }
        };
        chartPanel.setPreferredSize(new Dimension(0, 250));
        chartPanel.setBackground(ThemeManager.CARD);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        center.add(chartPanel, BorderLayout.NORTH);

        // Bottom: Detailed Table
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeManager.CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));

        String[] cols = {"Subject", "Exam Name", "Marks Obtained", "Max Marks", "Grade", "Date"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        marksTable = new JTable(model);
        styleTable(marksTable);

        JScrollPane scroll = new JScrollPane(marksTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeManager.CARD);
        tableCard.add(scroll, BorderLayout.CENTER);

        center.add(tableCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void drawBarChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartPanel.getWidth();
        int h = chartPanel.getHeight();
        int margin = 50;

        // Draw Axes
        g2.setColor(ThemeManager.DIVIDER);
        g2.drawLine(margin, h - margin, w - margin, h - margin); // X Axis
        g2.drawLine(margin, margin, margin, h - margin); // Y Axis

        if (currentMarks == null || currentMarks.isEmpty()) {
            g2.setColor(ThemeManager.SUB_TEXT);
            g2.setFont(new Font("SansSerif", Font.ITALIC, 14));
            g2.drawString("Student has not attended any evaluated tests", w/2 - 140, h/2);
            return;
        }

        // Calculate Average Marks Per Test
        java.util.Map<String, java.util.List<TestMark>> testGroups = new java.util.LinkedHashMap<>();
        for (TestMark m : currentMarks) {
            testGroups.computeIfAbsent(m.getTestName(), k -> new java.util.ArrayList<>()).add(m);
        }

        int barWidth = (w - 2*margin) / (testGroups.size() * 2);
        int x = margin + barWidth;
        int maxH = h - 2*margin;

        for (java.util.Map.Entry<String, java.util.List<TestMark>> entry : testGroups.entrySet()) {
            double totalObtained = 0;
            double totalMax = 0;
            for (TestMark m : entry.getValue()) {
                totalObtained += m.getMarksObtained();
                totalMax += m.getMaxMarks();
            }
            double avgPercent = (totalObtained / totalMax) * 100;

            // Draw Bar
            int barHeight = (int) (avgPercent / 100 * maxH);
            g2.setColor(new Color(59, 130, 246));
            g2.fillRoundRect(x, h - margin - barHeight, barWidth, barHeight, 8, 8);

            // Label Percentage
            g2.setColor(ThemeManager.TEXT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(String.format("%.0f%%", avgPercent), x + barWidth/2 - 12, h - margin - barHeight - 10);

            // Label Test Name
            g2.setColor(ThemeManager.SUB_TEXT);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String testName = entry.getKey();
            if(testName.length() > 10) testName = testName.substring(0, 8) + "..";
            g2.drawString(testName, x, h - margin + 20);

            x += barWidth * 2;
        }
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(45);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(ThemeManager.DIVIDER);
        
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        t.getTableHeader().setBackground(new Color(250, 251, 253));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=2; i<5; i++) t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
    }

    private void loadInitialData() {
        String parentId = SessionManager.getInstance().getUserId();
        linkedStudents = portalService.getLinkedStudents(parentId);
        for (Student s : linkedStudents) {
            studentSelector.addItem(s.getName());
        }
        if (!linkedStudents.isEmpty()) onStudentSelected();
    }

    private void onStudentSelected() {
        int idx = studentSelector.getSelectedIndex();
        if (idx >= 0) {
            currentStudent = linkedStudents.get(idx);
            loadMarks();
            chartPanel.repaint();
        }
    }

    private void loadMarks() {
        model.setRowCount(0);
        currentMarks = portalService.getDetailedMarks(currentStudent.getUserId());
        
        if (currentMarks.isEmpty()) {
            model.addRow(new Object[]{"—", "Student has not attended any evaluated tests", "—", "—", "—", "—"});
            chartPanel.repaint();
            return;
        }

        for (TestMark m : currentMarks) {
            model.addRow(new Object[]{
                m.getSubjectName(),
                m.getTestName(),
                m.getMarksObtained(),
                m.getMaxMarks(),
                m.getGrade() != null ? m.getGrade() : calculateGrade(m.getMarksObtained(), m.getMaxMarks()),
                m.getTestDate() != null ? new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a").format(m.getTestDate()) : "—"
            });
        }
        chartPanel.repaint();
    }

    private String calculateGrade(int o, int m) {
        if(m == 0) return "-";
        double p = (double)o/m*100;
        if(p >= 90) return "A+";
        if(p >= 80) return "A";
        if(p >= 70) return "B";
        if(p >= 60) return "C";
        return "F";
    }

    private void downloadReport() {
        if (currentStudent == null) return;
        if (currentMarks == null || currentMarks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No marks available for this student.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("ReportCard_" + currentStudent.getName().replace(" ", "_") + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = pdfService.generateReportCard(currentStudent, currentMarks, chooser.getSelectedFile().getAbsolutePath());
            if (path != null) {
                JOptionPane.showMessageDialog(this, "Report card downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to generate report card.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

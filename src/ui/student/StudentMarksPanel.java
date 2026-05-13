package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.BasicStroke;
import java.text.SimpleDateFormat;
import java.util.List;

import dao.TestsDAO;
import model.TestMark;
import util.SessionManager;

/**
 * StudentMarksPanel: Display student test marks in table and bar graph
 * 
 * FEATURES:
 * ✅ Table: Test name, subject, marks, percentage, grade
 * ✅ Bar Graph: Performance visualization by test
 * ✅ Auto-load marks on panel creation
 * ✅ Color-coded grades
 * ✅ Professional styling with Swing
 */
public class StudentMarksPanel extends JPanel {

    // Colors
    public static final Color PAGE_BG = new Color(244, 247, 249);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_PRI = new Color(26, 35, 64);
    public static final Color TEXT_SEC = new Color(107, 122, 153);
    public static final Color ACCENT = new Color(74, 144, 226);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(234, 179, 8);
    public static final Color ERROR = new Color(220, 38, 38);

    // UI Components
    private JTable marksTable;
    private DefaultTableModel tableModel;
    private JPanel graphPanel;
    private List<TestMark> marks;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy hh:mm a");

    public StudentMarksPanel() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        
        // Header
        add(createHeader(), BorderLayout.NORTH);
        
        // Main content
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(PAGE_BG);
        content.setBorder(new EmptyBorder(0, 24, 24, 24));
        
        // Table and Graph side by side
        JPanel tableCard = createTableSection();
        graphPanel = createGraphSection();
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableCard, graphPanel);
        split.setDividerLocation(600);
        split.setDividerSize(10);
        split.setBackground(PAGE_BG);
        content.add(split, BorderLayout.CENTER);
        
        add(content, BorderLayout.CENTER);
        
        // Load marks data
        loadMarksData();
    }

    // ─────────────────────────────────────────────────────────────
    // HEADER
    // ─────────────────────────────────────────────────────────────
    
    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BG);
        panel.setBorder(new EmptyBorder(24, 24, 12, 24));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);
        
        JLabel titleLbl = new JLabel("My Test Results");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(TEXT_PRI);
        
        JLabel subLbl = new JLabel("View your test performance and detailed marks analysis");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);
        
        titles.add(titleLbl);
        titles.add(subLbl);
        panel.add(titles, BorderLayout.WEST);
        
        // Refresh button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(PAGE_BG);
        JButton refreshBtn = createButton("↻ Refresh", 0, false);
        refreshBtn.addActionListener(e -> loadMarksData());
        right.add(refreshBtn);
        panel.add(right, BorderLayout.EAST);
        
        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // TABLE SECTION
    // ─────────────────────────────────────────────────────────────
    
    private JPanel createTableSection() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));

        // Create table with columns
        String[] columns = {"Test Name", "Subject", "Marks", "Percentage", "Grade"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        marksTable = new JTable(tableModel);
        marksTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        marksTable.setRowHeight(36);
        marksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        marksTable.setShowGrid(false);
        marksTable.setIntercellSpacing(new Dimension(0, 1));
        marksTable.setBackground(CARD_BG);
        marksTable.setGridColor(new Color(240, 243, 247));

        // Column widths
        marksTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        marksTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        marksTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        marksTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        marksTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        // Renderers
        marksTable.getColumnModel().getColumn(2).setCellRenderer(new MarksRenderer());
        marksTable.getColumnModel().getColumn(3).setCellRenderer(new PercentageRenderer());
        marksTable.getColumnModel().getColumn(4).setCellRenderer(new GradeRenderer());

        // Header
        marksTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        marksTable.getTableHeader().setBackground(new Color(242, 244, 247));
        marksTable.getTableHeader().setForeground(TEXT_PRI);
        marksTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 240)));

        JScrollPane scroll = new JScrollPane(marksTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(CARD_BG);

        card.add(scroll, BorderLayout.CENTER);
        
        // No data message
        JLabel emptyLbl = new JLabel("📊 No test results yet");
        emptyLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        emptyLbl.setForeground(TEXT_SEC);
        emptyLbl.setHorizontalAlignment(JLabel.CENTER);
        emptyLbl.setBorder(new EmptyBorder(40, 0, 40, 0));
        
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // GRAPH SECTION
    // ─────────────────────────────────────────────────────────────
    
    private JPanel createGraphSection() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));
        
        JLabel titleLbl = new JLabel("Performance Graph");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLbl.setForeground(TEXT_PRI);
        titleLbl.setBorder(new EmptyBorder(12, 12, 0, 12));
        card.add(titleLbl, BorderLayout.NORTH);
        
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph((Graphics2D) g, getWidth(), getHeight());
            }
        };
        graphPanel.setBackground(CARD_BG);
        graphPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.add(graphPanel, BorderLayout.CENTER);
        
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // LOAD DATA
    // ─────────────────────────────────────────────────────────────
    
    private void loadMarksData() {
        SwingWorker<List<TestMark>, Void> worker = new SwingWorker<List<TestMark>, Void>() {
            @Override
            protected List<TestMark> doInBackground() throws Exception {
                String studentId = SessionManager.getInstance().getUserId();
                if (studentId == null) {
                    System.err.println("[StudentMarksPanel] Student ID is null!");
                    return List.of();
                }
                
                TestsDAO testsDao = new TestsDAO();
                return testsDao.getStudentMarks(studentId);
            }

            @Override
            protected void done() {
                try {
                    marks = get();
                    updateTable();
                    graphPanel.repaint();
                } catch (Exception e) {
                    System.err.println("[StudentMarksPanel] Error loading marks: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE TABLE
    // ─────────────────────────────────────────────────────────────
    
    private void updateTable() {
        // Clear existing rows
        tableModel.setRowCount(0);
        
        if (marks == null || marks.isEmpty()) {
            // Show "no data" message
            return;
        }
        
        // Add rows
        for (TestMark tm : marks) {
            String marks = String.format("%d / %d", tm.getMarksObtained(), tm.getMaxMarks());
            String percentage = String.format("%.1f%%", tm.getPercentage());
            
            tableModel.addRow(new Object[]{
                tm.getTestName(),
                tm.getSubjectName(),
                marks,
                percentage,
                tm.getGrade()
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DRAW GRAPH
    // ─────────────────────────────────────────────────────────────
    
    private void drawGraph(Graphics2D g, int width, int height) {
        if (marks == null || marks.isEmpty()) {
            g.setColor(TEXT_SEC);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("No data to display", width / 2 - 50, height / 2);
            return;
        }

        int padding = 40;
        int graphWidth = width - 2 * padding;
        int graphHeight = height - 2 * padding;
        
        // Calculate bar dimensions
        int numBars = marks.size();
        int barWidth = Math.max(20, (graphWidth - 20) / numBars);
        int spacing = (graphWidth - barWidth * numBars) / (numBars + 1);

        // Draw axes
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(1));
        g.drawLine(padding, padding + graphHeight, padding, padding); // Y-axis
        g.drawLine(padding, padding + graphHeight, width - padding, padding + graphHeight); // X-axis

        // Draw grid lines and labels
        g.setColor(new Color(220, 220, 220));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        
        for (int i = 0; i <= 10; i++) {
            int y = padding + graphHeight - (i * graphHeight / 10);
            g.drawLine(padding - 3, y, width - padding, y);
            g.setColor(TEXT_SEC);
            g.drawString(i * 10 + "%", padding - 35, y + 3);
            g.setColor(new Color(220, 220, 220));
        }

        // Draw bars and labels
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        int x = padding + spacing;
        
        for (int i = 0; i < marks.size(); i++) {
            TestMark mark = marks.get(i);
            double percentage = mark.getPercentage();
            
            // Cap percentage at 100%
            if (percentage > 100) percentage = 100;
            
            int barHeight = (int) ((percentage / 100.0) * graphHeight);
            int barY = padding + graphHeight - barHeight;

            // Color based on grade
            Color barColor = getGradeColor(mark.getGrade());
            g.setColor(barColor);
            g.fillRect(x, barY, barWidth, barHeight);

            // Draw border
            g.setColor(new Color(100, 100, 100));
            g.setStroke(new BasicStroke(1));
            g.drawRect(x, barY, barWidth, barHeight);

            // Draw label
            g.setColor(TEXT_SEC);
            String label = Math.round(percentage) + "%";
            FontMetrics fm = g.getFontMetrics();
            int labelX = x + (barWidth - fm.stringWidth(label)) / 2;
            int labelY = barY - 5;
            g.drawString(label, labelX, labelY);

            // Draw test name below
            String testName = mark.getTestName();
            if (testName.length() > 8) {
                testName = testName.substring(0, 8) + "...";
            }
            g.setColor(TEXT_PRI);
            labelX = x + (barWidth - fm.stringWidth(testName)) / 2;
            labelY = padding + graphHeight + 15;
            g.drawString(testName, labelX, labelY);

            x += barWidth + spacing;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────────
    
    private Color getGradeColor(String grade) {
        if (grade != null) {
            if (grade.equals("A+") || grade.equals("A")) {
                return SUCCESS;
            } else if (grade.equals("B")) {
                return new Color(102, 194, 255);
            } else if (grade.equals("C")) {
                return WARNING;
            }
        }
        return ERROR;
    }

    private JButton createButton(String text, int type, boolean enabled) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setEnabled(enabled);
        
        if (type == 0) {
            btn.setBackground(new Color(230, 237, 247));
            btn.setForeground(ACCENT);
        }
        
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // CELL RENDERERS
    // ─────────────────────────────────────────────────────────────
    
    private class MarksRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            c.setFont(new Font("Monospaced", Font.BOLD, 12));
            return c;
        }
    }

    private class PercentageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            
            if (marks != null && row < marks.size()) {
                double percentage = marks.get(row).getPercentage();
                Color fgColor = TEXT_PRI;
                if (percentage >= 80) {
                    fgColor = StudentMarksPanel.SUCCESS;
                } else if (percentage >= 60) {
                    fgColor = StudentMarksPanel.WARNING;
                } else {
                    fgColor = StudentMarksPanel.ERROR;
                }
                c.setForeground(fgColor);
            }
            c.setFont(new Font("SansSerif", Font.BOLD, 11));
            return c;
        }
    }

    private class GradeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            
            String grade = (String) value;
            Color color = getGradeColor(grade);
            c.setForeground(color);
            c.setFont(new Font("SansSerif", Font.BOLD, 12));
            
            return c;
        }
    }
}

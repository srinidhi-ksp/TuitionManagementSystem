package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import model.*;
import service.StudentProfileService;
import util.SessionManager;

public class ProfilePanel extends JPanel {

    private JPanel contentPanel;
    private StudentProfileService profileService = new StudentProfileService();

    private static final Color ACCENT = new Color(74, 144, 226);
    private static final Color BG_COLOR = new Color(244, 247, 249);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRI = new Color(30, 40, 60);
    private static final Color TEXT_SEC = new Color(100, 110, 130);
    private static final Color SUCCESS = new Color(34, 197, 94);
    private static final Color ERROR = new Color(239, 68, 68);

    public ProfilePanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(TEXT_PRI);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_COLOR);

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadProfile();
    }

    private void loadProfile() {
        String userId = SessionManager.getInstance().getUserId();
        if (userId == null) return;

        new SwingWorker<StudentProfileDTO, Void>() {
            @Override
            protected StudentProfileDTO doInBackground() {
                return profileService.getStudentProfile(userId);
            }

            @Override
            protected void done() {
                try {
                    StudentProfileDTO data = get();
                    if (data == null) return;
                    renderProfile(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void renderProfile(StudentProfileDTO data) {
        contentPanel.removeAll();
        Student s = data.getStudent();

        // 1. Header Section
        contentPanel.add(createHeaderSection(s));
        contentPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 2. Personal & Academic (Split Row)
        JPanel detailsRow = new JPanel(new GridLayout(1, 2, 24, 0));
        detailsRow.setOpaque(false);
        detailsRow.add(createPersonalSection(s));
        detailsRow.add(createAcademicSection(s));
        contentPanel.add(detailsRow);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 3. Parent & Summary (Split Row)
        JPanel summaryRow = new JPanel(new GridLayout(1, 2, 24, 0));
        summaryRow.setOpaque(false);
        summaryRow.add(createParentSection(data));
        summaryRow.add(createSummarySection(data));
        contentPanel.add(summaryRow);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // 4. Test Performance Section
        contentPanel.add(createTestPerformanceSection(data.getTestPerformance()));
        
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createHeaderSection(Student s) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(20, 0));
        
        // Avatar
        JLabel avatar = new JLabel(s.getName().substring(0, 1).toUpperCase(), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        avatar.setFont(new Font("SansSerif", Font.BOLD, 32));
        avatar.setForeground(Color.WHITE);
        avatar.setPreferredSize(new Dimension(80, 80));
        card.add(avatar, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setOpaque(false);
        JLabel name = new JLabel(s.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 24));
        name.setForeground(TEXT_PRI);
        JLabel id = new JLabel("ID: " + s.getUserId() + "  |  Class: " + s.getCurrentStd() + "  |  Status: ACTIVE");
        id.setForeground(TEXT_SEC);
        info.add(name);
        info.add(id);
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    private JPanel createPersonalSection(Student s) {
        JPanel card = createSectionCard("Personal Details");
        JPanel grid = new JPanel(new GridLayout(0, 1, 0, 12));
        grid.setOpaque(false);
        grid.add(createField("Email", s.getEmail()));
        grid.add(createField("Phone", s.getPhone()));
        grid.add(createField("Address", s.getStreet() + ", " + s.getCity()));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAcademicSection(Student s) {
        JPanel card = createSectionCard("Academic Details");
        JPanel grid = new JPanel(new GridLayout(0, 1, 0, 12));
        grid.setOpaque(false);
        grid.add(createField("Join Date", s.getJoinDate() != null ? new SimpleDateFormat("dd MMM yyyy").format(s.getJoinDate()) : "N/A"));
        grid.add(createField("Board", s.getBoard()));
        grid.add(createField("Standard", s.getCurrentStd()));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createParentSection(StudentProfileDTO data) {
        JPanel card = createSectionCard("Parent / Guardian Details");
        JPanel grid = new JPanel(new GridLayout(0, 1, 0, 12));
        grid.setOpaque(false);
        grid.add(createField("Name", data.getParentName()));
        grid.add(createField("Relation", data.getParentRelation()));
        grid.add(createField("Contact", data.getParentPhone()));
        grid.add(createField("Occupation", data.getParentOccupation()));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSummarySection(StudentProfileDTO data) {
        JPanel card = createSectionCard("Fees & Enrollment Summary");
        JPanel grid = new JPanel(new GridLayout(0, 1, 0, 12));
        grid.setOpaque(false);
        
        Map<String, Object> fees = data.getFeeSummary();
        String feeStatus = fees != null ? (String) fees.get("status") : "N/A";
        
        grid.add(createField("Enrollments", data.getTotalEnrollments() + " Active Batches"));
        grid.add(createField("Fee Status", feeStatus));
        grid.add(createField("Pending Balance", fees != null ? "₹ " + fees.get("pendingAmount") : "N/A"));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTestPerformanceSection(TestPerformanceDTO perf) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);

        JLabel st = new JLabel("Test Performance");
        st.setFont(new Font("SansSerif", Font.BOLD, 20));
        st.setForeground(TEXT_PRI);
        st.setBorder(new EmptyBorder(0, 0, 16, 0));
        section.add(st);

        // Summary Cards Row
        JPanel summaryRow = new JPanel(new GridLayout(1, 4, 16, 0));
        summaryRow.setOpaque(false);
        summaryRow.add(createStatCard("Total Tests", String.valueOf(perf.getTotalTests()), ACCENT));
        summaryRow.add(createStatCard("Avg Score", String.format("%.1f%%", perf.getAverageScore()), SUCCESS));
        summaryRow.add(createStatCard("Highest", String.valueOf(perf.getHighestScore()), new Color(167, 139, 250)));
        summaryRow.add(createStatCard("Grade", perf.getGrade(), new Color(251, 146, 60)));
        section.add(summaryRow);
        section.add(Box.createRigidArea(new Dimension(0, 20)));

        // Performance Progress
        JPanel progressCard = createCard();
        progressCard.setLayout(new BorderLayout(15, 0));
        JLabel pLbl = new JLabel("Performance Progress");
        pLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        progressCard.add(pLbl, BorderLayout.NORTH);
        
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) perf.getAverageScore());
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(0, 24));
        bar.setForeground(perf.getAverageScore() >= 75 ? SUCCESS : (perf.getAverageScore() >= 50 ? Color.ORANGE : ERROR));
        progressCard.add(bar, BorderLayout.CENTER);
        section.add(progressCard);
        section.add(Box.createRigidArea(new Dimension(0, 24)));

        // Test History Table
        JPanel tableCard = createCard();
        tableCard.setLayout(new BorderLayout());
        JLabel hLbl = new JLabel("Test History");
        hLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        hLbl.setBorder(new EmptyBorder(0, 0, 15, 0));
        tableCard.add(hLbl, BorderLayout.NORTH);

        String[] cols = {"Test Name", "Score", "Total", "%", "Result"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (TestPerformanceDTO.TestHistoryItem item : perf.getTestHistory()) {
            model.addRow(new Object[]{
                item.getTestName(), item.getScore(), item.getTotalMarks(),
                String.format("%.1f%%", item.getPercentage()), item.getResult()
            });
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.getTableHeader().setBackground(new Color(245, 247, 250));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < 5; i++) table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, 200));
        tableScroll.setBorder(null);
        tableCard.add(tableScroll, BorderLayout.CENTER);
        section.add(tableCard);

        return section;
    }

    // Helper Methods
    private JPanel createCard() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 235, 245), 1, true),
            new EmptyBorder(20, 24, 20, 24)
        ));
        return p;
    }

    private JPanel createSectionCard(String title) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 15));
        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        tLbl.setForeground(ACCENT);
        card.add(tLbl, BorderLayout.NORTH);
        return card;
    }

    private JPanel createField(String label, String value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(TEXT_SEC);
        JLabel v = new JLabel(value != null && !value.isEmpty() ? value : "N/A");
        v.setFont(new Font("SansSerif", Font.BOLD, 14));
        v.setForeground(TEXT_PRI);
        p.add(l); p.add(v);
        return p;
    }

    private JPanel createStatCard(String label, String value, Color color) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 235, 245), 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT_SEC);
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 20));
        v.setForeground(color);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}

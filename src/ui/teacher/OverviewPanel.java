package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import model.Test;
import service.TeacherPortalService;
import util.SessionManager;

public class OverviewPanel extends JPanel {

    private Color bgLight = new Color(245, 247, 250);
    private JPanel statsPanel;
    private JPanel lowerPanel;
    private JLabel title;

    public OverviewPanel() {
        setLayout(new BorderLayout());
        setBackground(bgLight);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(bgLight);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        title = new JLabel("Welcome back!");
        title.setFont(new Font("Serif", Font.BOLD, 28));

        JLabel subtitle = new JLabel("Your Academic Dashboard Overview");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);

        header.add(title);
        header.add(subtitle);

        statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(bgLight);
        statsPanel.add(new JLabel("Loading stats..."));

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(bgLight);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(statsPanel, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);

        lowerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        lowerPanel.setBackground(bgLight);
        lowerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        lowerPanel.add(new JLabel("Loading..."));

        add(lowerPanel, BorderLayout.CENTER);

        loadOverviewDataAsync();
    }

    private JPanel createModernStatCard(String title, String value, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));

        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        JLabel iconLabel = new JLabel("*");
        iconLabel.setForeground(iconColor);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 30));

        card.add(textPanel, BorderLayout.CENTER);
        card.add(iconLabel, BorderLayout.EAST);

        return card;
    }

    private JPanel createTodayClassesCard(List<TeacherPortalService.ScheduleItem> classes) {
        JPanel card = createListCard("Today's Classes");
        JPanel list = (JPanel) ((JScrollPane) card.getComponent(1)).getViewport().getView();

        if (classes != null && !classes.isEmpty()) {
            for (TeacherPortalService.ScheduleItem item : classes) {
                JLabel row = new JLabel("- " + item.batch.getBatchName() + " (" + item.schedule.getStart() + " - " + item.schedule.getEnd() + ")");
                row.setForeground(Color.DARK_GRAY);
                row.setBorder(new EmptyBorder(5,0,5,0));
                list.add(row);
            }
        } else {
            JLabel empty = new JLabel("No classes scheduled today.");
            empty.setForeground(Color.GRAY);
            list.add(empty);
        }
        return card;
    }

    private JPanel createPendingTasksCard(List<Test> pendingTests) {
        JPanel card = createListCard("Action Items");
        JPanel list = (JPanel) ((JScrollPane) card.getComponent(1)).getViewport().getView();

        if (pendingTests != null && !pendingTests.isEmpty()) {
            for (Test test : pendingTests) {
                JLabel row = new JLabel("- " + test.getTestName() + " marks pending");
                row.setForeground(new Color(255, 150, 50));
                row.setBorder(new EmptyBorder(5,0,5,0));
                list.add(row);
            }
        } else {
            JLabel empty = new JLabel("No pending evaluations.");
            empty.setForeground(Color.GRAY);
            list.add(empty);
        }
        return card;
    }

    private JPanel createListCard(String heading) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel t = new JLabel(heading);
        t.setFont(new Font("Arial", Font.BOLD, 16));
        t.setBorder(new EmptyBorder(0,0,10,0));
        card.add(t, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private void loadOverviewDataAsync() {
        new SwingWorker<TeacherPortalService.DashboardData, Void>() {
            @Override
            protected TeacherPortalService.DashboardData doInBackground() {
                return new TeacherPortalService().getDashboardData(SessionManager.getCurrentTeacherId());
            }

            @Override
            protected void done() {
                try {
                    TeacherPortalService.DashboardData data = get();
                    String userName = SessionManager.getInstance().getUserName();
                    title.setText("Welcome back, " + (userName != null ? userName : "Teacher"));

                    statsPanel.removeAll();
                    statsPanel.add(createModernStatCard("Assigned Batches", String.valueOf(data.totalBatches), new Color(100, 150, 255)));
                    statsPanel.add(createModernStatCard("Total Students", String.valueOf(data.totalStudents), new Color(100, 200, 150)));
                    statsPanel.add(createModernStatCard("Pending Eval", String.valueOf(data.pendingEvaluations), new Color(180, 100, 255)));
                    statsPanel.add(createModernStatCard("Today's Classes", String.valueOf(data.todayClasses.size()), new Color(255, 100, 100)));
                    statsPanel.revalidate();
                    statsPanel.repaint();

                    lowerPanel.removeAll();
                    lowerPanel.add(createTodayClassesCard(data.todayClasses));
                    lowerPanel.add(createPendingTasksCard(data.pendingTests));
                    lowerPanel.revalidate();
                    lowerPanel.repaint();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(OverviewPanel.this, "Failed to load dashboard data: " + e.getMessage());
                }
            }
        }.execute();
    }
}

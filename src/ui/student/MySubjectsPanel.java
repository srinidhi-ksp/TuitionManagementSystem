package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import model.Batch;
import model.Subject;
import dao.EnrollmentDAO;
import dao.SubjectDAO;
import util.SessionManager;

public class MySubjectsPanel extends JPanel {

    private JPanel gridPanel;

    public MySubjectsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("My Enrolled Subjects");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(0, 3, 20, 20));
        gridPanel.setBackground(Color.WHITE);

        loadSubjectsAsync();

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSubjectCard(Subject s) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel(s.getSubjectName());
        title.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel category = new JLabel(s.getCategory() + " • " + s.getSyllabusVersion());
        category.setForeground(Color.GRAY);
        category.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(title);
        textPanel.add(category);
        
        card.add(textPanel, BorderLayout.NORTH);
        
        JTextArea desc = new JTextArea(s.getDescription() != null ? s.getDescription() : "No description provided.");
        desc.setWrapStyleWord(true);
        desc.setLineWrap(true);
        desc.setEditable(false);
        desc.setForeground(Color.DARK_GRAY);
        desc.setBackground(Color.WHITE);
        card.add(desc, BorderLayout.CENTER);
        
        JButton btn = new JButton("View Syllabus");
        btn.setBackground(new Color(245, 245, 250));
        btn.setFocusPainted(false);
        card.add(btn, BorderLayout.SOUTH);

        return card;
    }

    private void loadSubjectsAsync() {
        gridPanel.removeAll();
        JLabel loadLbl = new JLabel("Loading...");
        loadLbl.setForeground(Color.GRAY);
        gridPanel.add(loadLbl);

        new SwingWorker<List<Subject>, Void>() {
            @Override
            protected List<Subject> doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                List<Subject> subjects = new java.util.ArrayList<>();
                if (userId == null) return subjects;

                List<Batch> enrolled = new EnrollmentDAO().getBatchesByStudentId(userId);
                Set<Integer> subjectIds = new HashSet<>();
                
                if (enrolled != null) {
                    for (Batch b : enrolled) {
                        subjectIds.add(b.getSubjectId());
                    }
                }

                SubjectDAO subDao = new SubjectDAO();
                for (Integer sid : subjectIds) {
                    Subject s = subDao.getSubjectById(sid);
                    if (s != null) subjects.add(s);
                }
                return subjects;
            }

            @Override
            protected void done() {
                try {
                    List<Subject> subjects = get();
                    gridPanel.removeAll();
                    if (subjects.isEmpty()) {
                        gridPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                        JLabel empty = new JLabel("No subjects found. Wait for teacher assignments.");
                        empty.setForeground(Color.GRAY);
                        gridPanel.add(empty);
                    } else {
                        gridPanel.setLayout(new GridLayout(0, 3, 20, 20));
                        for (Subject s : subjects) {
                            gridPanel.add(createSubjectCard(s));
                        }
                    }
                    gridPanel.revalidate();
                    gridPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    gridPanel.removeAll();
                    JLabel errorLbl = new JLabel("Error loading subjects.");
                    errorLbl.setForeground(Color.RED);
                    gridPanel.add(errorLbl);
                    gridPanel.revalidate();
                    gridPanel.repaint();
                }
            }
        }.execute();
    }
}

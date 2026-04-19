package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import model.User;
import model.Batch;
import model.Student;
import model.Enrollment;
import model.Test;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.TestsDAO;
import java.util.Date;
import java.text.SimpleDateFormat;

public class TestsMarksPanel extends JPanel {

    private User teacherContext;
    private JComboBox<String> batchSelector;
    private JTextField testNameField;
    private JTextField testDateField;
    private JTextField maxMarksField;

    private JComboBox<String> testSelector;
    private List<Test> loadedTests;

    private JTable marksTable;
    private DefaultTableModel marksModel;
    private List<Batch> myBatches;
    private List<Student> currentStudents;
    private int selectedTestId = -1;

    public TestsMarksPanel(User user) {
        this.teacherContext = user;
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        JLabel title = new JLabel("Tests & Marks");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        myBatches = new BatchDAO().getBatchesByTeacherId(teacherContext.getUserId());

        // Create Test Panel
        JPanel createPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        createPanel.setBackground(new Color(245, 245, 250));
        createPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(15, 15, 15, 15)
        ));

        batchSelector = new JComboBox<>();
        batchSelector.addItem("-- Select Batch --");
        if(myBatches != null) {
            for(Batch b : myBatches) {
                batchSelector.addItem(b.getBatchName());
            }
        }
        
        testNameField = new JTextField("Midterm Exam");
        testDateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        maxMarksField = new JTextField("100");

        JButton createBtn = new JButton("Create Test");
        createBtn.setBackground(new Color(30, 190, 160));
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.addActionListener(e -> createTestRecord());

        createPanel.add(new JLabel("Batch:"));
        createPanel.add(batchSelector);
        createPanel.add(new JLabel("Test Name:"));
        createPanel.add(testNameField);
        createPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        createPanel.add(testDateField);
        createPanel.add(new JLabel("Max Marks:"));
        createPanel.add(maxMarksField);
        createPanel.add(new JLabel("")); // spacer
        createPanel.add(createBtn);

        // Grade Entry Panel
        JPanel gradePanel = new JPanel(new BorderLayout());
        gradePanel.setBackground(Color.WHITE);
        gradePanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel gradeHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gradeHeader.setBackground(Color.WHITE);
        
        testSelector = new JComboBox<>();
        testSelector.addItem("-- Load Batch Tests First --");
        
        JButton loadTestsBtn = new JButton("Load Tests for Batch");
        loadTestsBtn.addActionListener(e -> loadExamsForBatch());

        JButton loadStudentsBtn = new JButton("Load Students");
        loadStudentsBtn.addActionListener(e -> populateGradeSheet());

        gradeHeader.add(loadTestsBtn);
        gradeHeader.add(testSelector);
        gradeHeader.add(loadStudentsBtn);
        gradePanel.add(gradeHeader, BorderLayout.NORTH);

        String columns[] = {"Student ID", "Student Name", "Marks Obtained (-1 for Absent)"};
        marksModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only marks editable
            }
        };
        
        marksTable = new JTable(marksModel);
        marksTable.setRowHeight(40);
        marksTable.setIntercellSpacing(new Dimension(0, 0));
        marksTable.setShowHorizontalLines(true);
        marksTable.setGridColor(new Color(230, 230, 230));

        JScrollPane scrollPane = new JScrollPane(marksTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        gradePanel.add(scrollPane, BorderLayout.CENTER);
        
        JButton saveMarksBtn = new JButton("Save Marks");
        saveMarksBtn.setPreferredSize(new Dimension(160, 40));
        saveMarksBtn.setBackground(new Color(30, 190, 160));
        saveMarksBtn.setForeground(Color.WHITE);
        saveMarksBtn.addActionListener(e -> saveMarksToDB());
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        footer.add(saveMarksBtn);
        gradePanel.add(footer, BorderLayout.SOUTH);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(Color.WHITE);
        centerContainer.add(createPanel, BorderLayout.NORTH);
        centerContainer.add(gradePanel, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);
    }

    private void createTestRecord() {
        if(batchSelector.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Select a valid batch."); return;
        }

        try {
            Batch b = myBatches.get(batchSelector.getSelectedIndex() - 1);
            Test t = new Test();
            t.setTestId((int)(Math.random()*100000));
            t.setBatchId(b.getBatchId());
            t.setTestName(testNameField.getText());
            t.setTestDate(new SimpleDateFormat("yyyy-MM-dd").parse(testDateField.getText()));
            t.setMaxMarks(Integer.parseInt(maxMarksField.getText()));

            if (new TestsDAO().addTest(t)) {
                JOptionPane.showMessageDialog(this, "Test Created Successfully!");
                loadExamsForBatch();
            } else {
                JOptionPane.showMessageDialog(this, "Database Insert Failed.");
            }
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid fields provided. Check Date and Max Marks.");
        }
    }

    private void loadExamsForBatch() {
        if(batchSelector.getSelectedIndex() <= 0) return;
        Batch b = myBatches.get(batchSelector.getSelectedIndex() - 1);
        loadedTests = new TestsDAO().getTestsByBatchId(b.getBatchId());
        
        testSelector.removeAllItems();
        if(loadedTests != null && !loadedTests.isEmpty()) {
            for(Test t : loadedTests) {
                testSelector.addItem(t.getTestName() + " (Max: " + t.getMaxMarks() + ")");
            }
        } else {
            testSelector.addItem("-- No Tests Found --");
        }
    }

    private void populateGradeSheet() {
        if(testSelector.getSelectedIndex() < 0 || loadedTests == null || loadedTests.isEmpty()) return;
        
        selectedTestId = loadedTests.get(testSelector.getSelectedIndex()).getTestId();
        Batch selectedBatch = myBatches.get(batchSelector.getSelectedIndex() - 1);
        
        marksModel.setRowCount(0);
        currentStudents = new ArrayList<>();
        List<Enrollment> enrollments = new EnrollmentDAO().getAllEnrollments();
        List<Student> allStudents = new StudentDAO().getAllStudents();
        
        for(Enrollment e : enrollments) {
            if(e.getBatchId() == selectedBatch.getBatchId()) {
                for(Student s : allStudents) {
                    if(s.getUserId().equals(e.getStudentUserId())) {
                        currentStudents.add(s);
                        marksModel.addRow(new Object[]{ s.getUserId(), s.getName(), "0" });
                        break;
                    }
                }
            }
        }
    }

    private void saveMarksToDB() {
        if(selectedTestId == -1 || currentStudents == null || currentStudents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid test or students loaded.");
            return;
        }
        
        if (marksTable.isEditing()) {
            marksTable.getCellEditor().stopCellEditing();
        }

        TestsDAO tDao = new TestsDAO();
        int max = loadedTests.get(testSelector.getSelectedIndex()).getMaxMarks();
        int success = 0;

        try {
            for(int i = 0; i < marksTable.getRowCount(); i++) {
                String sId = (String) marksModel.getValueAt(i, 0);
                String marksStr = marksModel.getValueAt(i, 2).toString();
                int m = Integer.parseInt(marksStr);

                if (m > max) {
                    JOptionPane.showMessageDialog(this, "Error at row " + (i+1) + ": Marks cannot exceed max " + max);
                    return;
                }

                if(tDao.saveMark(selectedTestId, sId, m)) {
                    success++;
                }
            }
            JOptionPane.showMessageDialog(this, "Successfully saved " + success + " mark entries to DB.");
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid mark format. Ensure all marks are integers.");
        }
    }
}

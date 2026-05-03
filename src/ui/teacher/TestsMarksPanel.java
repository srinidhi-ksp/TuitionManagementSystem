package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.Batch;
import model.Test;
import model.User;
import service.TeacherPortalService;
import util.SessionManager;

public class TestsMarksPanel extends JPanel {

    private JComboBox<String> batchSelector;
    private JTextField testNameField;
    private JTextField testDateField;
    private JTextField maxMarksField;
    private JComboBox<String> testSelector;
    private List<Test> loadedTests = new ArrayList<>();
    private JTable marksTable;
    private DefaultTableModel marksModel;
    private List<Batch> myBatches = new ArrayList<>();
    private List<TeacherPortalService.MarkRow> currentRows = new ArrayList<>();
    private int selectedTestId = -1;
    private TeacherPortalService service = new TeacherPortalService();

    public TestsMarksPanel(User user) {
        setLayout(new BorderLayout(0, 20));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        JLabel title = new JLabel("Tests & Marks");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel createPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        createPanel.setBackground(new Color(245, 245, 250));
        createPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(15, 15, 15, 15)
        ));

        batchSelector = new JComboBox<>();
        batchSelector.addItem("Loading batches...");

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
        createPanel.add(new JLabel(""));
        createPanel.add(createBtn);

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

        String columns[] = {"Student ID", "Student Name", "Marks Obtained"};
        marksModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 2; }
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

        loadBatchesAsync();
    }

    private void loadBatchesAsync() {
        new SwingWorker<List<Batch>, Void>() {
            @Override protected List<Batch> doInBackground() {
                return service.getTeacherBatches(SessionManager.getCurrentTeacherId());
            }
            @Override protected void done() {
                try {
                    myBatches = get();
                    batchSelector.removeAllItems();
                    batchSelector.addItem("-- Select Batch --");
                    for (Batch batch : myBatches) batchSelector.addItem(batch.getBatchName());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TestsMarksPanel.this, "Failed to load batches: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void createTestRecord() {
        if(batchSelector.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Select a valid batch."); return;
        }
        try {
            Batch batch = myBatches.get(batchSelector.getSelectedIndex() - 1);
            Date testDate = parseDate(testDateField.getText().trim());
            if (testDate.after(new Date())) {
                JOptionPane.showMessageDialog(this, "Test date cannot be in the future."); return;
            }
            int totalMarks = Integer.parseInt(maxMarksField.getText().trim());
            if (totalMarks <= 0 || totalMarks > 200) {
                JOptionPane.showMessageDialog(this, "Total marks must be between 1 and 200."); return;
            }
            String testName = testNameField.getText().trim();
            if (testName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Test name is required."); return;
            }

            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() {
                    return service.createTest(SessionManager.getCurrentTeacherId(), batch.getBatchId(), testName, testDate, totalMarks);
                }
                @Override protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(TestsMarksPanel.this, "Test Created Successfully!");
                            loadExamsForBatch();
                        } else {
                            JOptionPane.showMessageDialog(TestsMarksPanel.this, "Database Insert Failed.");
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(TestsMarksPanel.this, "Database Insert Failed: " + e.getMessage());
                    }
                }
            }.execute();
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid fields provided. Check Date and Max Marks.");
        }
    }

    private void loadExamsForBatch() {
        if(batchSelector.getSelectedIndex() <= 0) return;
        Batch batch = myBatches.get(batchSelector.getSelectedIndex() - 1);
        testSelector.removeAllItems();
        testSelector.addItem("Loading...");

        new SwingWorker<List<Test>, Void>() {
            @Override protected List<Test> doInBackground() {
                return service.getTestsForBatch(SessionManager.getCurrentTeacherId(), batch.getBatchId());
            }
            @Override protected void done() {
                try {
                    loadedTests = get();
                    testSelector.removeAllItems();
                    if(loadedTests != null && !loadedTests.isEmpty()) {
                        for(Test test : loadedTests) testSelector.addItem(test.getTestName() + " (Max: " + test.getMaxMarks() + ")");
                    } else {
                        testSelector.addItem("-- No Tests Found --");
                    }
                } catch (Exception e) {
                    testSelector.removeAllItems();
                    testSelector.addItem("-- Load Failed --");
                    JOptionPane.showMessageDialog(TestsMarksPanel.this, "Failed to load tests: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void populateGradeSheet() {
        if(testSelector.getSelectedIndex() < 0 || loadedTests == null || loadedTests.isEmpty()) return;
        Test selectedTest = loadedTests.get(testSelector.getSelectedIndex());
        selectedTestId = selectedTest.getTestId();
        marksModel.setRowCount(0);
        marksModel.addRow(new Object[]{"Loading...", "Loading students...", ""});

        new SwingWorker<List<TeacherPortalService.MarkRow>, Void>() {
            @Override protected List<TeacherPortalService.MarkRow> doInBackground() {
                return service.getMarkRows(SessionManager.getCurrentTeacherId(), selectedTest);
            }
            @Override protected void done() {
                try {
                    currentRows = get();
                    marksModel.setRowCount(0);
                    if (currentRows.isEmpty()) {
                        marksModel.addRow(new Object[]{"N/A", "No active students", ""});
                        return;
                    }
                    for (TeacherPortalService.MarkRow row : currentRows) {
                        marksModel.addRow(new Object[]{
                            row.student.getUserId(),
                            row.student.getName(),
                            row.existingScore != null ? String.valueOf(row.existingScore) : ""
                        });
                    }
                } catch (Exception e) {
                    marksModel.setRowCount(0);
                    JOptionPane.showMessageDialog(TestsMarksPanel.this, "Failed to load students: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void saveMarksToDB() {
        if(selectedTestId == -1 || currentRows == null || currentRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid test or students loaded.");
            return;
        }
        if (marksTable.isEditing()) marksTable.getCellEditor().stopCellEditing();

        Test selectedTest = loadedTests.get(testSelector.getSelectedIndex());
        List<MarkSave> saves = new ArrayList<>();
        try {
            for(int i = 0; i < marksTable.getRowCount(); i++) {
                String studentId = String.valueOf(marksModel.getValueAt(i, 0));
                String value = String.valueOf(marksModel.getValueAt(i, 2)).trim();
                if (value.isEmpty()) continue;
                int score = Integer.parseInt(value);
                if (score < 0 || score > selectedTest.getMaxMarks()) {
                    JOptionPane.showMessageDialog(this, "Marks for student " + studentId + " must be between 0 and " + selectedTest.getMaxMarks());
                    return;
                }
                saves.add(new MarkSave(studentId, score));
            }
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid mark format. Ensure all marks are integers.");
            return;
        }

        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                int success = 0;
                String teacherId = SessionManager.getCurrentTeacherId();
                for (MarkSave save : saves) {
                    if (service.saveMark(teacherId, selectedTest, save.studentId, save.score)) success++;
                }
                return success;
            }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(TestsMarksPanel.this, "Successfully saved " + get() + " mark entries to DB.");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TestsMarksPanel.this, "Failed to save marks: " + e.getMessage());
                }
            }
        }.execute();
    }

    private Date parseDate(String text) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setLenient(false);
        return fmt.parse(text);
    }

    private static class MarkSave {
        String studentId;
        int score;
        MarkSave(String studentId, int score) {
            this.studentId = studentId;
            this.score = score;
        }
    }
}

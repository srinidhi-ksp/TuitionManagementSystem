package ui.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import dao.SubjectDAO;
import model.Subject;

public class SubjectManagementFrame extends JPanel {

    private JTable subjectTable;
    private DefaultTableModel model;
    private List<Subject> currentSubjects;

    public SubjectManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        JLabel title = new JLabel("Subject Management");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        panel.add(title, BorderLayout.WEST);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(Color.WHITE);
        
        JTextField searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(220, 40));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200,200,200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.setText("\uD83D\uDD0D Search records...");
        searchField.setForeground(Color.GRAY);

        JButton addBtn = new JButton("+ Add New Subject");
        addBtn.setBackground(new Color(30, 190, 160));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("Arial", Font.BOLD, 14));
        addBtn.setPreferredSize(new Dimension(170, 40));
        addBtn.addActionListener(e -> openAddSubjectModal());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setBackground(new Color(30, 190, 160));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 14));
        refreshBtn.setPreferredSize(new Dimension(100, 40));
        refreshBtn.addActionListener(e -> refreshTable());
        
        rightPanel.add(searchField);
        rightPanel.add(refreshBtn);
        rightPanel.add(addBtn);
        
        panel.add(rightPanel, BorderLayout.EAST);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private JPanel createBody() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(createTablePanel(), BorderLayout.CENTER);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private JScrollPane createTablePanel() {
        String columns[] = {"Subject Name", "Category", "Syllabus Version", "Monthly Fee", "Status", "Actions"};
        model = new DefaultTableModel(columns, 0);
        subjectTable = new JTable(model);
        subjectTable.setRowHeight(40);
        subjectTable.setIntercellSpacing(new Dimension(0, 0));
        subjectTable.setShowGrid(false);
        subjectTable.setShowHorizontalLines(true);
        subjectTable.setGridColor(new Color(230, 230, 230));

        refreshTable();
        
        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onEdit(int row) {
                openAddSubjectModal();
            }
            @Override
            public void onDelete(int row) {
                if (subjectTable.isEditing()) {
                    subjectTable.getCellEditor().stopCellEditing();
                }
                int confirm = JOptionPane.showConfirmDialog(null, 
                    "Are you sure you want to delete this record?", 
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Subject subject = currentSubjects.get(row);
                    boolean s = new SubjectDAO().deleteSubject(subject.getSubjectId());
                    if (s) {
                        refreshTable();
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to delete.");
                    }
                }
            }
        };
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender());
        subjectTable.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(event));

        JScrollPane scrollPane = new JScrollPane(subjectTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private void refreshTable() {
        model.setRowCount(0);
        currentSubjects = new SubjectDAO().getAllSubjects();
        for (Subject s : currentSubjects) {
            model.addRow(new Object[]{
                s.getSubjectName(), s.getCategory(), s.getSyllabusVersion(), 
                "Rs. " + s.getMonthlyFee(), s.getStatus(), ""
            });
        }
    }

    private JPanel createInputPanel(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        JLabel l = new JLabel(labelText);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setForeground(new Color(80, 80, 80));
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, 35));
        if(comp instanceof JTextField) {
            ((JTextField)comp).setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)),
                BorderFactory.createEmptyBorder(5,10,5,10)
            ));
        }
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void openAddSubjectModal() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Subject", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 20, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.setBackground(Color.WHITE);

        JTextField nameField = new JTextField();
        formPanel.add(createInputPanel("Subject Name", nameField));
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Science", "Mathematics", "Commerce", "Languages", "Arts"});
        formPanel.add(createInputPanel("Category", categoryCombo));
        JTextField versionField = new JTextField("v1.0");
        formPanel.add(createInputPanel("Syllabus Version", versionField));
        JTextField feeField = new JTextField();
        formPanel.add(createInputPanel("Monthly Fee", feeField));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active", "Inactive"});
        formPanel.add(createInputPanel("Status", statusCombo));
        
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200,200,200)),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        formPanel.add(createInputPanel("Description", new JScrollPane(descArea)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(new Color(245, 245, 245));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(100, 35));
        cancelBtn.setBackground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        
        JButton submitBtn = new JButton("Submit");
        submitBtn.setPreferredSize(new Dimension(120, 35));
        submitBtn.setBackground(new Color(30, 190, 160));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);

        cancelBtn.addActionListener(e -> dialog.dispose());
        submitBtn.addActionListener(e -> {
            try {
                Subject s = new Subject();
                // Random ID for simplicity since sequence is not clearly defined
                s.setSubjectId((int)(Math.random() * 1000));
                s.setSubjectName(nameField.getText());
                s.setCategory(categoryCombo.getSelectedItem().toString());
                s.setSyllabusVersion(versionField.getText());
                s.setMonthlyFee(Double.parseDouble(feeField.getText()));
                s.setStatus(statusCombo.getSelectedItem().toString());
                s.setDescription(descArea.getText());

                if(new SubjectDAO().addSubject(s)) {
                   JOptionPane.showMessageDialog(dialog, "Subject Added.");
                   refreshTable();
                   dialog.dispose();
                } else {
                   JOptionPane.showMessageDialog(dialog, "Failed to add.");
                }
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid info.");
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(submitBtn);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
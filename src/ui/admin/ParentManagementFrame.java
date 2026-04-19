package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import dao.StudentDAO;
import model.Student;

/**
 * ParentManagementFrame
 *
 * ARCHITECTURE: Parent data is embedded inside the students collection.
 * We use StudentDAO as the SINGLE SOURCE OF TRUTH — no separate ParentDAO query.
 *
 * student.parent = { parent_id, full_name, phone, occupation }
 */
public class ParentManagementFrame extends JPanel {

    private JTable parentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    // Snapshot of loaded students (for search filtering)
    private List<Student> allStudents;

    public ParentManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
    }

    // ── Header ─────────────────────────────────────────────────────────────────

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 10, 20));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Parent Management");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        panel.add(title, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(Color.WHITE);

        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(220, 40));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.setText("\uD83D\uDD0D Search by parent or student...");
        searchField.setForeground(Color.GRAY);

        // Live search
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String query = searchField.getText().toLowerCase().trim();
                filterTable(query);
            }
        });
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getForeground().equals(Color.GRAY)) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
        });

        JButton refreshBtn = new JButton("⟳ Refresh");
        refreshBtn.setBackground(new Color(30, 190, 160));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 14));
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.addActionListener(e -> refreshTable());

        rightPanel.add(searchField);
        rightPanel.add(refreshBtn);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    // ── Body / Table ───────────────────────────────────────────────────────────

    private JPanel createBody() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // Info banner
        JLabel infoLabel = new JLabel(
            "<html><body style='color:#666;font-size:11px'>" +
            "ℹ  Parent details are sourced from <b>students.parent</b> (embedded document). " +
            "Each row represents a student–parent pair." +
            "</body></html>"
        );
        infoLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 230, 255)),
            new EmptyBorder(8, 12, 8, 12)
        ));
        infoLabel.setBackground(new Color(235, 248, 255));
        infoLabel.setOpaque(true);
        panel.add(infoLabel, BorderLayout.NORTH);
        panel.add(createTableScrollPane(), BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createTableScrollPane() {
        String[] columns = {
            "Parent Name", "Student Name (ID)", "Relation", "Contact No.", "Occupation"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        parentTable = new JTable(tableModel);
        parentTable.setRowHeight(42);
        parentTable.setFont(new Font("Arial", Font.PLAIN, 13));
        parentTable.setIntercellSpacing(new Dimension(0, 0));
        parentTable.setShowGrid(false);
        parentTable.setShowHorizontalLines(true);
        parentTable.setGridColor(new Color(230, 230, 230));

        // Header styling
        parentTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        parentTable.getTableHeader().setBackground(new Color(245, 247, 250));
        parentTable.getTableHeader().setForeground(new Color(80, 80, 80));
        parentTable.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200))
        );

        // Alternate row colouring
        parentTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 0));
                if (isSelected) {
                    setBackground(new Color(220, 245, 240));
                    setForeground(new Color(20, 150, 130));
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 251, 252));
                    setForeground(Color.DARK_GRAY);
                }
                return this;
            }
        });

        refreshTable();

        JScrollPane scrollPane = new JScrollPane(parentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    // ── Data Loading ───────────────────────────────────────────────────────────

    private void refreshTable() {
        tableModel.setRowCount(0);

        // ✅ SINGLE SOURCE OF TRUTH: students collection, embedded parent
        allStudents = new StudentDAO().getAllStudents();

        int loaded = 0;
        for (Student s : allStudents) {
            // Only add rows where parent data exists
            if (s.getParentName() == null || "N/A".equals(s.getParentName())) continue;

            tableModel.addRow(new Object[]{
                s.getParentName(),
                s.getName() + " (" + s.getUserId() + ")",
                s.getParentRelation(),
                s.getParentPhone(),
                s.getParentOccupation()
            });
            loaded++;
        }

        if (loaded == 0) {
            // Show a hint row if nothing loaded
            tableModel.addRow(new Object[]{
                "No parent data found", "—", "—", "—", "—"
            });
        }
    }

    private void filterTable(String query) {
        tableModel.setRowCount(0);
        if (allStudents == null) return;

        for (Student s : allStudents) {
            if (s.getParentName() == null || "N/A".equals(s.getParentName())) continue;

            boolean matches = query.isEmpty()
                || s.getParentName().toLowerCase().contains(query)
                || (s.getName() != null && s.getName().toLowerCase().contains(query))
                || (s.getUserId() != null && s.getUserId().toLowerCase().contains(query));

            if (matches) {
                tableModel.addRow(new Object[]{
                    s.getParentName(),
                    s.getName() + " (" + s.getUserId() + ")",
                    s.getParentRelation(),
                    s.getParentPhone(),
                    s.getParentOccupation()
                });
            }
        }
    }
}

package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import service.FeeService;
import dao.SubjectDAO;
import model.Subject;

/**
 * Payment Dialog for recording subject-wise payments
 * Dialog to confirm and record payment when marking a subject as paid
 */
public class PaymentDialog extends JDialog {

    private FeeService feeService;
    private SubjectDAO subjectDAO;
    private Runnable onPaymentSuccess;
    
    private String studentId;
    private int subjectId;
    private double feeAmount;
    
    private JComboBox<String> paymentModeCombo;
    private JLabel studentNameLabel;
    private JLabel subjectNameLabel;
    private JLabel feeLabel;
    private JLabel dateLabel;

    private static final Color ACCENT_COLOR = new Color(74, 144, 226);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color BG_COLOR = new Color(244, 247, 249);

    public PaymentDialog(JFrame parent, String studentId, String studentName, String subjectName, 
                        double fee, Runnable onSuccess) {
        super(parent, "Record Payment", true);
        
        this.studentId = studentId;
        this.feeAmount = fee;
        this.onPaymentSuccess = onSuccess;
        
        this.feeService = new FeeService();
        this.subjectDAO = new SubjectDAO();

        setSize(500, 450);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        // Get subject ID from name (fetch first subject with this name)
        for (Subject s : subjectDAO.getAllSubjects()) {
            if (s.getSubjectName().equals(subjectName)) {
                this.subjectId = s.getSubjectId();
                break;
            }
        }

        createUI(studentName, subjectName);
    }

    private void createUI(String studentName, String subjectName) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = createContentPanel(studentName, subjectName);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Record Payment");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(26, 35, 64));

        JLabel subtitleLabel = new JLabel("Confirm payment details and mark subject as paid");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(107, 122, 153));

        panel.add(titleLabel);
        panel.add(subtitleLabel);

        return panel;
    }

    private JPanel createContentPanel(String studentName, String subjectName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        // Student Name Field
        panel.add(createLabeledField("Student Name:", studentName, true));
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Subject Name Field
        panel.add(createLabeledField("Subject Name:", subjectName, true));
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Fee Amount Field
        panel.add(createLabeledField("Fee Amount:", String.format("Rs. %.2f", feeAmount), true));
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Payment Date Field
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        String currentDate = sdf.format(new Date());
        panel.add(createLabeledField("Payment Date:", currentDate, true));
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Payment Mode Dropdown
        JLabel modeLabel = new JLabel("Payment Mode:");
        modeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        modeLabel.setForeground(new Color(26, 35, 64));
        modeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        paymentModeCombo = new JComboBox<>(new String[]{"Cash", "UPI", "Card"});
        paymentModeCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        paymentModeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        paymentModeCombo.setBackground(Color.WHITE);
        paymentModeCombo.setFocusable(true);

        panel.add(modeLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(paymentModeCombo);

        return panel;
    }

    private JPanel createLabeledField(String labelText, String value, boolean readOnly) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(BG_COLOR);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(26, 35, 64));
        label.setPreferredSize(new Dimension(120, 0));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        valueLabel.setForeground(new Color(70, 80, 100));
        valueLabel.setOpaque(true);
        valueLabel.setBackground(Color.WHITE);
        valueLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));

        panel.add(label, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        panel.setBackground(BG_COLOR);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 12));
        cancelBtn.setBackground(new Color(200, 200, 200));
        cancelBtn.setForeground(Color.DARK_GRAY);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
            new EmptyBorder(10, 25, 10, 25)
        ));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("✓ Mark as Paid");
        saveBtn.setFont(new Font("Arial", Font.BOLD, 12));
        saveBtn.setBackground(SUCCESS_COLOR);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SUCCESS_COLOR, 1, true),
            new EmptyBorder(10, 25, 10, 25)
        ));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> handleSave());

        panel.add(cancelBtn);
        panel.add(saveBtn);

        return panel;
    }

    private void handleSave() {
        try {
            String paymentMode = (String) paymentModeCombo.getSelectedItem();
            String month = new SimpleDateFormat("yyyy-MM").format(new Date());

            // Mark subject as paid
            boolean success = feeService.markSubjectAsPaid(
                studentId,
                subjectId,
                feeAmount,
                paymentMode,
                month
            );

            if (success) {
                JOptionPane.showMessageDialog(
                    this,
                    "Payment recorded successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // Callback to refresh parent table
                if (onPaymentSuccess != null) {
                    onPaymentSuccess.run();
                }

                dispose();
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to record payment. Subject may already be marked as paid.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Exception e) {
            System.err.println("[PaymentDialog] Error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

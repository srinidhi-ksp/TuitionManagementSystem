package ui.student;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.Calendar;
import java.util.Date;
import model.Payment;
import service.FeeService;
import util.ThemeManager;

public class PaymentDialog extends JDialog {
    private final String studentId;
    private final int batchId;
    private final double amount;
    private final String subjectName;
    private final Runnable onSuccess;
    private final FeeService feeService;

    private String selectedMethod = "UPI";
    private JTextField upiInput = new JTextField();
    private JTextField cardInput = new JTextField();
    private JTextField expiryInput = new JTextField("MM/YY");
    private JTextField cvvInput = new JTextField();

    public PaymentDialog(Frame parent, String studentId, int batchId, double amount, String subjectName, Runnable onSuccess) {
        super(parent, "Secure Payment - " + subjectName, true);
        this.studentId = studentId;
        this.batchId = batchId;
        this.amount = amount;
        this.subjectName = subjectName;
        this.onSuccess = onSuccess;
        this.feeService = new FeeService();

        initializeUI();
    }

    private void initializeUI() {
        setSize(500, 600);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 5));
        header.setOpaque(false);
        JLabel title = new JLabel("Complete Your Payment");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel subtitle = new JLabel(subjectName + " - Amount: Rs. " + amount);
        subtitle.setForeground(Color.GRAY);
        header.add(title);
        header.add(subtitle);
        mainPanel.add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel(new CardLayout());
        content.setOpaque(false);
        
        JPanel methodSelection = new JPanel(new GridLayout(3, 1, 0, 10));
        methodSelection.setOpaque(false);
        
        JButton upiBtn = createMethodButton("UPI Payment", "Pay using any UPI App");
        JButton cardBtn = createMethodButton("Credit/Debit Card", "All major cards accepted");
        JButton cashBtn = createMethodButton("Cash at Office", "Request cash payment approval");

        upiBtn.addActionListener(e -> showPaymentForm("UPI", content));
        cardBtn.addActionListener(e -> showPaymentForm("CARD", content));
        cashBtn.addActionListener(e -> showPaymentForm("CASH", content));

        methodSelection.add(upiBtn);
        methodSelection.add(cardBtn);
        methodSelection.add(cashBtn);

        mainPanel.add(methodSelection, BorderLayout.CENTER);

        // Footer
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        mainPanel.add(cancelBtn, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JButton createMethodButton(String title, String sub) {
        JButton btn = new JButton("<html><div style='padding:10px;'><b>" + title + "</b><br><font color='gray'>" + sub + "</font></div></html>");
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBackground(new Color(250, 251, 253));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(230, 233, 237)));
        return btn;
    }

    private void showPaymentForm(String type, JPanel container) {
        selectedMethod = type;
        container.removeAll();
        container.setLayout(new BorderLayout(0, 20));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        if ("UPI".equals(type)) {
            JLabel upiLabel = new JLabel("Enter UPI ID (e.g., name@upi)");
            upiLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            upiInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            form.add(upiLabel);
            form.add(Box.createVerticalStrut(10));
            form.add(upiInput);
        } else if ("CARD".equals(type)) {
            cardInput.setToolTipText("Card Number");
            expiryInput.setToolTipText("MM/YY");
            cvvInput.setToolTipText("CVV");
            form.add(new JLabel("Card Number"));
            form.add(cardInput);
            form.add(Box.createVerticalStrut(10));
            form.add(new JLabel("Expiry (MM/YY)"));
            form.add(expiryInput);
            form.add(Box.createVerticalStrut(10));
            form.add(new JLabel("CVV"));
            form.add(cvvInput);
        } else {
            form.add(new JLabel("<html><center>Request approval for cash payment.<br>Please pay at the office counter after requesting.</center></html>"));
        }

        JButton payBtn = new JButton(type.equals("CASH") ? "REQUEST APPROVAL" : "PAY NOW - Rs. " + amount);
        payBtn.setBackground(new Color(59, 130, 246));
        payBtn.setForeground(Color.WHITE);
        payBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        payBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        payBtn.addActionListener(e -> processPayment(payBtn));

        container.add(form, BorderLayout.CENTER);
        container.add(payBtn, BorderLayout.SOUTH);
        
        container.revalidate();
        container.repaint();
    }

    private void processPayment(JButton payBtn) {
        // Simple Validation
        if ("UPI".equals(selectedMethod) && !upiInput.getText().contains("@")) {
            JOptionPane.showMessageDialog(this, "Invalid UPI ID", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        payBtn.setText("Processing...");
        payBtn.setEnabled(false);

        new Timer(1500, e -> {
            boolean success = feeService.recordPayment(studentId, batchId, selectedMethod);
            if (success) {
                dispose();
                JOptionPane.showMessageDialog(null, "Payment Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                if (onSuccess != null) onSuccess.run();
            } else {
                payBtn.setText("Try Again");
                payBtn.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Payment Failed. This batch may already be paid.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }
}

package ui.student;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import model.Receipt;
import util.PDFReceiptGenerator;
import util.ThemeManager;

public class ReceiptDialog extends JDialog {
    private Receipt receipt;
    private JTextArea receiptArea;

    public ReceiptDialog(Frame parent, Receipt receipt) {
        super(parent, "Fee Receipt", true);
        this.receipt = receipt;
        
        setSize(500, 650);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeManager.isDarkMode ? new Color(30, 30, 40) : Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        receiptArea.setBackground(ThemeManager.isDarkMode ? new Color(40, 40, 50) : new Color(250, 250, 250));
        receiptArea.setForeground(ThemeManager.isDarkMode ? Color.WHITE : Color.BLACK);
        receiptArea.setMargin(new Insets(20, 20, 20, 20));
        
        String content = generateReceiptText();
        receiptArea.setText(content);

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.isDarkMode ? Color.GRAY : Color.LIGHT_GRAY));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        JButton downloadBtn = createStyledButton("Download PDF", new Color(34, 197, 94));
        downloadBtn.addActionListener(e -> downloadPDF());

        JButton printBtn = createStyledButton("Print", new Color(59, 130, 246));
        printBtn.addActionListener(e -> {
            try {
                receiptArea.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton closeBtn = createStyledButton("Close", Color.GRAY);
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(downloadBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(closeBtn);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private String generateReceiptText() {
        StringBuilder sb = new StringBuilder();
        sb.append("      MRK TUITION CENTER\n");
        sb.append("         FEE RECEIPT\n");
        sb.append("==========================================\n\n");
        sb.append(String.format("%-16s : %s\n", "Student Name", receipt.getStudentName()));
        sb.append(String.format("%-16s : %s\n", "Student ID", receipt.getStudentId()));
        sb.append(String.format("%-16s : %s\n", "Class", receipt.getClassName()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-16s : %s\n", "Batch Name", receipt.getBatchName()));
        sb.append(String.format("%-16s : %s\n", "Subject", receipt.getSubjectName()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-16s : \u20b9%.2f\n", "Amount Paid", receipt.getAmount()));
        sb.append(String.format("%-16s : %s\n", "Payment Date", receipt.getPaymentDate()));
        sb.append(String.format("%-16s : %s\n", "Payment Mode", receipt.getPaymentMode()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-16s : %s\n", "Status", receipt.getStatus()));
        sb.append("==========================================\n\n");
        sb.append("          Thank You!\n");
        return sb.toString();
    }

    private void downloadPDF() {
        try {
            PDFReceiptGenerator generator = new PDFReceiptGenerator();
            String fileName = generator.generateReceipt(receipt);
            
            if (fileName != null) {
                JOptionPane.showMessageDialog(this, 
                    "Professional receipt generated successfully!\n\nFile: " + fileName, 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to generate PDF receipt.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating PDF: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}

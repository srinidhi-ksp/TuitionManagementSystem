package ui.parent;

import java.awt.*;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import model.Payment;
import model.Student;
import model.SubjectFeeDTO;
import service.FeeService;
import service.PDFReportService;
import service.ParentPortalService;
import util.SessionManager;
import util.ThemeManager;

/**
 * Parent Fees Panel - Razorpay-style Payment Flow
 */
public class ParentFeesPanel extends JPanel {

    private FeeService feeService;
    private ParentPortalService portalService;
    private PDFReportService pdfService;
    private JComboBox<String> studentSelector;
    private List<Student> linkedStudents;
    private Student currentStudent;
    
    private JLabel totalLbl, paidLbl, pendingLbl;

    public ParentFeesPanel() {
        this.feeService = new FeeService();
        this.portalService = new ParentPortalService();
        this.pdfService = new PDFReportService();
        
        setLayout(new BorderLayout(0, 30));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initContent();
        loadInitialData();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("Fees & Payments");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("Manage tuition fees and download payment receipts");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titles.add(title);
        titles.add(sub);

        studentSelector = new JComboBox<>();
        studentSelector.setPreferredSize(new Dimension(220, 38));
        studentSelector.addActionListener(e -> onStudentSelected());

        header.add(titles, BorderLayout.WEST);
        header.add(studentSelector, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel center = new JPanel(new BorderLayout(0, 30));
        center.setBackground(ThemeManager.BG);

        // Summary Card
        JPanel summaryCard = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryCard.setBackground(ThemeManager.BG);
        
        totalLbl = new JLabel("₹0");
        paidLbl = new JLabel("₹0");
        pendingLbl = new JLabel("₹0");

        summaryCard.add(createSummaryMiniCard("TOTAL FEES", totalLbl, new Color(59, 130, 246)));
        summaryCard.add(createSummaryMiniCard("PAID AMOUNT", paidLbl, new Color(34, 197, 94)));
        summaryCard.add(createSummaryMiniCard("PENDING", pendingLbl, new Color(239, 68, 68)));

        center.add(summaryCard, BorderLayout.NORTH);

        // Payment Methods Redesign (Razorpay Style)
        JPanel payContainer = new JPanel(new GridBagLayout());
        payContainer.setBackground(ThemeManager.BG);
        
        JPanel gatewayCard = new JPanel(new BorderLayout(0, 25));
        gatewayCard.setBackground(ThemeManager.CARD);
        gatewayCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(30, 35, 35, 35)
        ));
        gatewayCard.setPreferredSize(new Dimension(500, 450));

        JLabel payTitle = new JLabel("Secure Payment Gateway");
        payTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        payTitle.setForeground(ThemeManager.TEXT);
        gatewayCard.add(payTitle, BorderLayout.NORTH);

        JPanel methodsGrid = new JPanel(new GridLayout(3, 1, 0, 15));
        methodsGrid.setOpaque(false);

        methodsGrid.add(createMethodBtn("⚡ Pay via UPI", "Instant payment via Google Pay, PhonePe, Paytm", "UPI"));
        methodsGrid.add(createMethodBtn("💳 Credit / Debit Card", "Securely pay using your Visa, Master or RuPay card", "CARD"));
        methodsGrid.add(createMethodBtn("💵 Cash at Counter", "Generate payment slip to pay in person at the office", "CASH"));

        gatewayCard.add(methodsGrid, BorderLayout.CENTER);
        
        JLabel footer = new JLabel("🔒 256-bit SSL Secure Encrypted Payment", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setForeground(ThemeManager.SUB_TEXT);
        gatewayCard.add(footer, BorderLayout.SOUTH);

        payContainer.add(gatewayCard);
        center.add(payContainer, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
    }

    private JPanel createSummaryMiniCard(String title, JLabel val, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(ThemeManager.CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(20, 24, 20, 24)
        ));
        
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setForeground(ThemeManager.SUB_TEXT);
        
        val.setFont(new Font("SansSerif", Font.BOLD, 20));
        val.setForeground(accent);
        
        p.add(t, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private JButton createMethodBtn(String title, String sub, String type) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(15, 0));
        btn.setBackground(new Color(248, 250, 252));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(12, 20, 12, 20)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 15));
        t.setForeground(ThemeManager.TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(new Font("SansSerif", Font.PLAIN, 11));
        s.setForeground(ThemeManager.SUB_TEXT);
        text.add(t);
        text.add(s);

        btn.add(text, BorderLayout.CENTER);
        btn.add(new JLabel("›"), BorderLayout.EAST);

        btn.addActionListener(e -> showPaymentGateway(type));
        
        return btn;
    }

    private void showPaymentGateway(String type) {
        if (currentStudent == null) return;
        
        // Find unpaid subject
        List<SubjectFeeDTO> details = feeService.getStudentFeeDetails(currentStudent.getUserId());
        SubjectFeeDTO toPay = null;
        for (SubjectFeeDTO d : details) {
            if ("UNPAID".equals(d.getPaymentStatus())) {
                toPay = d;
                break;
            }
        }

        if (toPay == null) {
            JOptionPane.showMessageDialog(this, "No pending fees found for " + currentStudent.getName(), "No Payments Due", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((JFrame)SwingUtilities.getWindowAncestor(this), "Complete Payment", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(0, 20));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel h = new JLabel("Payable: ₹" + toPay.getMonthlyFee());
        h.setFont(new Font("SansSerif", Font.BOLD, 22));
        h.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(h, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);

        if ("UPI".equals(type)) {
            JLabel qrLbl = new JLabel("Scan QR Code to Pay", SwingConstants.CENTER);
            qrLbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
            content.add(qrLbl, BorderLayout.NORTH);
            
            JPanel qr = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(Color.BLACK);
                    for(int i=0; i<15; i++) {
                        for(int j=0; j<15; j++) {
                            if((i*j+i+j)%4 == 0) g.fillRect(i*10+45, j*10+20, 10, 10);
                        }
                    }
                }
            };
            qr.setPreferredSize(new Dimension(200, 200));
            content.add(qr, BorderLayout.CENTER);
        } else if ("CARD".equals(type)) {
            JPanel cardForm = new JPanel(new GridLayout(4, 1, 0, 15));
            cardForm.setOpaque(false);
            cardForm.add(new JTextField("1234 5678 9876 5432"));
            cardForm.add(new JTextField("MM / YY"));
            cardForm.add(new JTextField("CVV"));
            cardForm.add(new JLabel("Cardholder Name"));
            content.add(cardForm, BorderLayout.CENTER);
        } else {
            JLabel msg = new JLabel("<html><center>Please visit the office counter to pay in cash.<br><br><b>Reference ID: " + System.currentTimeMillis() % 100000 + "</b></center></html>");
            msg.setHorizontalAlignment(SwingConstants.CENTER);
            content.add(msg, BorderLayout.CENTER);
        }

        JButton payBtn = new JButton("Confirm Payment");
        payBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        payBtn.setBackground(new Color(59, 130, 246));
        payBtn.setForeground(Color.WHITE);
        payBtn.setPreferredSize(new Dimension(0, 45));
        
        final SubjectFeeDTO target = toPay;
        payBtn.addActionListener(e -> {
            payBtn.setText("Processing...");
            payBtn.setEnabled(false);
            
            Timer t = new Timer(1500, ex -> {
                boolean success = feeService.recordPayment(currentStudent.getUserId(), target.getSubjectId(), type);
                if (success) {
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "🎉 Payment Successful for " + target.getSubjectName(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    onStudentSelected();
                    
                    Payment pObj = new Payment();
                    pObj.setAmountPaid(target.getMonthlyFee());
                    pObj.setPaymentMode(type);
                    pObj.setPaymentDate(new Date());
                    pObj.setSubjectId(target.getSubjectName());
                    
                    int down = JOptionPane.showConfirmDialog(this, "Download Receipt?", "Success", JOptionPane.YES_NO_OPTION);
                    if (down == JOptionPane.YES_OPTION) downloadReceipt(pObj);
                }
            });
            t.setRepeats(false);
            t.start();
        });

        p.add(content, BorderLayout.CENTER);
        p.add(payBtn, BorderLayout.SOUTH);
        dialog.add(p);
        dialog.setVisible(true);
    }

    private void loadInitialData() {
        linkedStudents = portalService.getLinkedStudents(SessionManager.getInstance().getUserId());
        studentSelector.removeAllItems();
        for (Student s : linkedStudents) studentSelector.addItem(s.getName());
        if (!linkedStudents.isEmpty()) onStudentSelected();
    }

    private void onStudentSelected() {
        int idx = studentSelector.getSelectedIndex();
        if (idx >= 0) {
            currentStudent = linkedStudents.get(idx);
            Map<String, Object> summary = feeService.getFeeSummary(currentStudent.getUserId());
            totalLbl.setText(String.format("₹%.0f", summary.get("totalFee")));
            paidLbl.setText(String.format("₹%.0f", summary.get("paidAmount")));
            pendingLbl.setText(String.format("₹%.0f", summary.get("pendingAmount")));
        }
    }

    private void downloadReceipt(Payment p) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Receipt_" + currentStudent.getName().replace(" ", "_") + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = pdfService.generateReceipt(currentStudent, p, chooser.getSelectedFile().getAbsolutePath());
            if (path != null) {
                JOptionPane.showMessageDialog(this, "Receipt downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}

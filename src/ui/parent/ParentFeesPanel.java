package ui.parent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import model.Payment;
import model.Student;
import model.SubjectFeeDTO;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import dao.PaymentDAO;
import dao.StudentDAO;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import service.FeeService;
import service.PDFReportService;
import service.ParentPortalService;
import util.SessionManager;
import util.ThemeManager;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javax.swing.SwingWorker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import db.DBConnection;
/**
 * Parent Fees Panel - Razorpay-style Payment Flow
 */
public class ParentFeesPanel extends JPanel {

    private FeeService feeService;
    private ParentPortalService portalService;
    private PDFReportService pdfService;

    private static final Color CARD_BG = Color.WHITE;
    private JComboBox<String> studentSelector;
    private List<Student> linkedStudents;
    private Student currentStudent;
    
    private JLabel totalLbl, paidLbl, pendingLbl;
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private PaymentDAO paymentDAO;
    private StudentDAO studentDAO;

    public ParentFeesPanel() {
        this.feeService = new FeeService();
        this.portalService = new ParentPortalService();
        this.pdfService = new PDFReportService();
        this.paymentDAO = new PaymentDAO();
        this.studentDAO = new StudentDAO();
        
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
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(ThemeManager.BG);
        scrollContent.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Summary Card
        JPanel summaryCard = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryCard.setBackground(ThemeManager.BG);
        summaryCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        totalLbl = new JLabel("₹0");
        paidLbl = new JLabel("₹0");
        pendingLbl = new JLabel("₹0");

        summaryCard.add(createSummaryMiniCard("TOTAL FEES", totalLbl, new Color(59, 130, 246)));
        summaryCard.add(createSummaryMiniCard("PAID AMOUNT", paidLbl, new Color(34, 197, 94)));
        summaryCard.add(createSummaryMiniCard("PENDING", pendingLbl, new Color(239, 68, 68)));

        scrollContent.add(summaryCard);
        scrollContent.add(Box.createRigidArea(new Dimension(0, 30)));

        // Payment Methods Redesign (Razorpay Style)
        JPanel payContainer = new JPanel(new GridBagLayout());
        payContainer.setBackground(ThemeManager.BG);
        payContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
        
        JPanel gatewayCard = new JPanel(new BorderLayout(0, 0));
        gatewayCard.setBackground(CARD_BG);
        gatewayCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));
        gatewayCard.setPreferredSize(new Dimension(500, 480));

        // Modern Header for Gateway
        JPanel gatewayHeader = new JPanel(new BorderLayout());
        gatewayHeader.setBackground(new Color(10, 27, 63)); // Dark blue
        gatewayHeader.setBorder(new EmptyBorder(25, 30, 25, 30));
        
        JLabel gatewayTitle = new JLabel("Secure Payment");
        gatewayTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        gatewayTitle.setForeground(Color.WHITE);
        gatewayHeader.add(gatewayTitle, BorderLayout.WEST);
        
        JLabel lockIcon = new JLabel("🔒 SSL SECURE");
        lockIcon.setFont(new Font("SansSerif", Font.BOLD, 10));
        lockIcon.setForeground(new Color(255, 255, 255, 120));
        gatewayHeader.add(lockIcon, BorderLayout.EAST);
        
        gatewayCard.add(gatewayHeader, BorderLayout.NORTH);

        JPanel methodsPanel = new JPanel(new BorderLayout());
        methodsPanel.setBackground(Color.WHITE);
        methodsPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel selectLabel = new JLabel("SELECT PAYMENT METHOD");
        selectLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        selectLabel.setForeground(ThemeManager.SUB_TEXT);
        selectLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        methodsPanel.add(selectLabel, BorderLayout.NORTH);

        JPanel methodsGrid = new JPanel(new GridLayout(3, 1, 0, 15));
        methodsGrid.setOpaque(false);

        methodsGrid.add(createMethodBtn("⚡ Pay via UPI", "Instant payment via GPay, PhonePe, Paytm", "UPI"));
        methodsGrid.add(createMethodBtn("💳 Credit / Debit Card", "Visa, Mastercard, RuPay supported", "CARD"));
        methodsGrid.add(createMethodBtn("💵 Cash at Counter", "Pay at office and get instant receipt", "CASH"));

        methodsPanel.add(methodsGrid, BorderLayout.CENTER);
        gatewayCard.add(methodsPanel, BorderLayout.CENTER);
        
        payContainer.add(gatewayCard);
        scrollContent.add(payContainer);
        scrollContent.add(Box.createRigidArea(new Dimension(0, 30)));

        // Payment History Section
        JPanel historySection = new JPanel(new BorderLayout(0, 15));
        historySection.setBackground(ThemeManager.BG);
        historySection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        
        JPanel historyHeader = new JPanel(new BorderLayout());
        historyHeader.setBackground(ThemeManager.BG);
        JLabel historyTitle = new JLabel("Payment History");
        historyTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        historyTitle.setForeground(ThemeManager.TEXT);
        
        JButton downloadBtn = new JButton("Download Receipt");
        downloadBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        downloadBtn.addActionListener(e -> {
            // Get the selected row index from the payment history table
            int selectedRow = historyTable.getSelectedRow();
            
            // Also check if user just clicked any row without explicitly 
            // selecting — try to use last clicked row as fallback
            if (selectedRow < 0 && historyTable.getRowCount() > 0) {
                // If only one row exists, auto-select it
                if (historyTable.getRowCount() == 1) {
                    historyTable.setRowSelectionInterval(0, 0);
                    selectedRow = 0;
                }
            }
            
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(
                    ParentFeesPanel.this,
                    "Please select a payment row from the table first.",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Read values directly from the selected table row
            // Column order: Date | Batch | Amount | Method | Status
            String date   = historyTable.getValueAt(selectedRow, 0) != null
                          ? historyTable.getValueAt(selectedRow, 0).toString() : "—";
            String batch  = historyTable.getValueAt(selectedRow, 1) != null
                          ? historyTable.getValueAt(selectedRow, 1).toString() : "—";
            String amount = historyTable.getValueAt(selectedRow, 2) != null
                          ? historyTable.getValueAt(selectedRow, 2).toString() : "—";
            String method = historyTable.getValueAt(selectedRow, 3) != null
                          ? historyTable.getValueAt(selectedRow, 3).toString() : "—";
            String status = historyTable.getValueAt(selectedRow, 4) != null
                          ? historyTable.getValueAt(selectedRow, 4).toString() : "—";
            
            // Skip if this is the "no records found" placeholder row
            if (batch.contains("No payment records found")) {
                JOptionPane.showMessageDialog(
                    ParentFeesPanel.this,
                    "No valid payment selected.",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            generateAndDownloadReceipt(date, batch, amount, method, status);
        });
        
        historyHeader.add(historyTitle, BorderLayout.WEST);
        historyHeader.add(downloadBtn, BorderLayout.EAST);
        historySection.add(historyHeader, BorderLayout.NORTH);

        String[] cols = {"Date", "Batch", "Amount", "Method", "Status", "PaymentObj"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(35);
        historyTable.getColumnModel().removeColumn(historyTable.getColumnModel().getColumn(5)); // Hide Payment object
        
        historyTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        historyTable.setCellSelectionEnabled(false);
        historyTable.setRowSelectionAllowed(true);

        JScrollPane tableScroll = new JScrollPane(historyTable);
        tableScroll.setPreferredSize(new Dimension(0, 300));
        tableScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        historySection.add(tableScroll, BorderLayout.CENTER);

        scrollContent.add(historySection);

        JScrollPane mainScroll = new JScrollPane(scrollContent);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(ThemeManager.BG);
        center.add(mainScroll, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
    }

    private Payment getSelectedPayment() {
        int row = historyTable.getSelectedRow();
        if (row == -1) return null;
        return (Payment) historyModel.getValueAt(row, 5);
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
        
        // Fetch fresh fee details
        List<SubjectFeeDTO> details = feeService.getStudentFeeDetails(currentStudent.getUserId());
        java.util.List<SubjectFeeDTO> unpaid = new java.util.ArrayList<>();
        for (SubjectFeeDTO d : details) {
            if ("UNPAID".equalsIgnoreCase(d.getPaymentStatus()) || "PENDING".equalsIgnoreCase(d.getPaymentStatus())) {
                unpaid.add(d);
            }
        }

        if (unpaid.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No pending fees found for " + currentStudent.getName(), "No Payments Due", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Selection Dialog for Batch
        SubjectFeeDTO toPay = unpaid.get(0);
        if (unpaid.size() > 1) {
            String[] choices = new String[unpaid.size()];
            for (int i = 0; i < unpaid.size(); i++) choices[i] = unpaid.get(i).getSubjectName() + " (₹" + unpaid.get(i).getMonthlyFee() + ")";
            String selected = (String) JOptionPane.showInputDialog(this, "Select Subject/Batch to Pay:", "Choose Batch", 
                                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
            if (selected == null) return;
            for (int i = 0; i < choices.length; i++) {
                if (choices[i].equals(selected)) {
                    toPay = unpaid.get(i);
                    break;
                }
            }
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

        // Declare validation fields
        final JTextField upiInput = new JTextField(SessionManager.getInstance().getUserName().split(" ")[0].toLowerCase() + "@upi");
        final JTextField cardInput = new JTextField("1234 5678 9876 5432");
        final JTextField expiryInput = new JTextField("MM / YY");
        final JTextField cvvInput = new JTextField("CVV");

        if ("UPI".equals(type)) {
            p.setBackground(new Color(250, 251, 254)); // Light bluish background
            
            JLabel upiHeader = new JLabel("UPI PAYMENT", SwingConstants.CENTER);
            upiHeader.setFont(new Font("SansSerif", Font.BOLD, 18));
            upiHeader.setForeground(new Color(30, 41, 59));
            p.add(upiHeader, BorderLayout.NORTH);

            JPanel upiContent = new JPanel(new GridLayout(1, 2, 20, 0));
            upiContent.setOpaque(false);

            // Left Side: QR Code
            JPanel qrSide = new JPanel(new BorderLayout(0, 10));
            qrSide.setOpaque(false);
            qrSide.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

            JLabel qrImgLabel = new JLabel();
            qrImgLabel.setHorizontalAlignment(SwingConstants.CENTER);
            try {
                String upiString = "upi://pay?pa=tuition@upi&pn=MRK Tuition&am=" + toPay.getMonthlyFee() + "&cu=INR";
                String encoded = URLEncoder.encode(upiString, StandardCharsets.UTF_8.toString());
                String apiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + encoded;
                
                // Fetch image from API
                BufferedImage image = ImageIO.read(new URL(apiUrl));
                if (image != null) {
                    qrImgLabel.setIcon(new ImageIcon(image));
                } else {
                    qrImgLabel.setText("QR API Offline");
                }
            } catch (Exception e) {
                qrImgLabel.setText("QR Generation Failed");
                System.err.println("[ParentFeesPanel] QR Error: " + e.getMessage());
            }
            qrImgLabel.setPreferredSize(new Dimension(180, 180));
            qrImgLabel.setOpaque(true);
            qrImgLabel.setBackground(Color.WHITE);
            qrImgLabel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

            JLabel amountLabel = new JLabel("Rs. " + toPay.getMonthlyFee(), SwingConstants.CENTER);
            amountLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            
            JLabel upiIdLabel = new JLabel("UPI ID: tuition@upi", SwingConstants.CENTER);
            upiIdLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            upiIdLabel.setForeground(Color.GRAY);

            qrSide.add(qrImgLabel, BorderLayout.CENTER);
            JPanel qrText = new JPanel(new GridLayout(2, 1));
            qrText.setOpaque(false);
            qrText.add(amountLabel);
            qrText.add(upiIdLabel);
            qrSide.add(qrText, BorderLayout.SOUTH);

            // Right Side: UPI Apps & ID Input
            JPanel inputSide = new JPanel();
            inputSide.setLayout(new BoxLayout(inputSide, BoxLayout.Y_AXIS));
            inputSide.setOpaque(false);

            JLabel appsLabel = new JLabel("Pay using UPI Apps");
            appsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            appsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            inputSide.add(appsLabel);
            inputSide.add(Box.createVerticalStrut(10));

            JPanel iconsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            iconsRow.setOpaque(false);
            iconsRow.add(new JLabel("GPay"));
            iconsRow.add(new JLabel("PhonePe"));
            iconsRow.add(new JLabel("Paytm"));
            iconsRow.add(new JLabel("BHIM"));
            iconsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            inputSide.add(iconsRow);
            inputSide.add(Box.createVerticalStrut(20));

            JLabel enterLabel = new JLabel("Enter UPI ID");
            enterLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            enterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            inputSide.add(enterLabel);
            
            upiInput.setPreferredSize(new Dimension(200, 35));
            upiInput.setMaximumSize(new Dimension(200, 35));
            upiInput.setAlignmentX(Component.LEFT_ALIGNMENT);
            inputSide.add(Box.createVerticalStrut(5));
            inputSide.add(upiInput);
            
            upiContent.add(qrSide);
            upiContent.add(inputSide);
            content.add(upiContent, BorderLayout.CENTER);

        } else if ("CARD".equals(type)) {
            JPanel cardForm = new JPanel(new GridLayout(4, 1, 0, 15));
            cardForm.setOpaque(false);
            cardForm.add(cardInput);
            cardForm.add(expiryInput);
            cardForm.add(cvvInput);
            cardForm.add(new JLabel("Cardholder Name: " + SessionManager.getInstance().getUserName()));
            content.add(cardForm, BorderLayout.CENTER);
        } else {
            JLabel msg = new JLabel("<html><center>Please visit the office counter to pay in cash.<br><br><b>Reference ID: " + System.currentTimeMillis() % 100000 + "</b></center></html>");
            msg.setHorizontalAlignment(SwingConstants.CENTER);
            content.add(msg, BorderLayout.CENTER);
        }

        JButton payBtn = new JButton("VERIFY AND PAY");
        if ("CASH".equals(type)) {
            payBtn.setText("REQUEST PAYMENT");
        }
        payBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        payBtn.setBackground(new Color(59, 130, 246));
        payBtn.setForeground(Color.WHITE);
        payBtn.setPreferredSize(new Dimension(0, 50));
        
        final SubjectFeeDTO target = toPay;
        payBtn.addActionListener(e -> {
            // Validation
            if ("UPI".equals(type)) {
                String upi = upiInput.getText().trim();
                // Accept any valid UPI ID format: non-empty text @ non-empty text
                if (!upi.matches("[\\w.\\-]+@[\\w.\\-]+")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid UPI ID format. Example: name@upi or number@bank", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if ("CARD".equals(type)) {
                String cardNum = cardInput.getText().replaceAll("\\s+", "");
                String expiry = expiryInput.getText().trim();
                String cvv = cvvInput.getText().trim();
                
                if (!cardNum.matches("\\d{12}")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid Card Number. Must be 12 digits.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!cvv.matches("\\d{3}")) {
                    JOptionPane.showMessageDialog(dialog, "Invalid CVV. Must be 3 digits.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Future expiry check (basic MM/YY)
                try {
                    String[] parts = expiry.split("/");
                    int m = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt("20" + parts[1]);
                    Calendar cal = Calendar.getInstance();
                    if (y < cal.get(Calendar.YEAR) || (y == cal.get(Calendar.YEAR) && m <= cal.get(Calendar.MONTH) + 1)) {
                        JOptionPane.showMessageDialog(dialog, "Card Expired", "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Invalid Expiry Format (MM/YY)", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            payBtn.setText("Processing...");
            payBtn.setEnabled(false);
            
            Timer t = new Timer(1500, ex -> {
                boolean success = feeService.recordPayment(currentStudent.getUserId(), target.getBatchId(), type);

                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        dialog.dispose();
                        String msg = "CASH".equals(type)
                            ? "Cash payment request submitted. Awaiting admin approval."
                            : "\uD83C\uDF89 Payment Successful for " + target.getSubjectName();
                        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);

                        onStudentSelected(); // Force UI reload from DB
                        revalidate();
                        repaint();

                        if (!"CASH".equals(type)) {
                            int down = JOptionPane.showConfirmDialog(this, "Download Receipt?", "Success", JOptionPane.YES_NO_OPTION);
                            if (down == JOptionPane.YES_OPTION) {
                                Payment fetchedPayment = new dao.PaymentDAO().getPayment(currentStudent.getUserId(), target.getBatchId());
                                if(fetchedPayment != null) downloadReceipt(fetchedPayment);
                            }
                        }
                    } else {
                        payBtn.setText("VERIFY AND PAY");
                        payBtn.setEnabled(true);
                        // insertPayment() returns false for duplicate as well as DB failure
                        JOptionPane.showMessageDialog(dialog,
                            "Payment not recorded.\nThis batch may have already been paid for this month, or a DB error occurred.",
                            "Payment Failed", JOptionPane.WARNING_MESSAGE);
                    }
                });
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
            
            loadPaymentHistory(currentStudent.getUserId());
        }
    }

    private void loadPaymentHistory(String studentId) {
        historyModel.setRowCount(0);
        if (studentId == null || studentId.isEmpty()) return;

        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    MongoDatabase db = DBConnection.getDatabase();
                    if (db == null) return null;

                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                    MongoCollection<Document> paymentsCol =
                        db.getCollection("payments");
                    MongoCollection<Document> enrollmentsCol =
                        db.getCollection("enrollments");
                    MongoCollection<Document> batchesCol =
                        db.getCollection("batches");
                    MongoCollection<Document> subjectsCol =
                        db.getCollection("subjects");

                    // ────────────────────────────────────────────────────────
                    // FORMAT 1: enrollment-based payments
                    // These have "enrollment_id" field and NO "student_id"
                    // ────────────────────────────────────────────────────────
                    FindIterable<Document> enrollments = enrollmentsCol.find(
                        new Document("student_id", studentId)
                    );

                    for (Document enrollment : enrollments) {
                        Object enrId = enrollment.get("_id");
                        if (enrId == null) continue;

                        // Strictly only Format-1: has enrollment_id, 
                        // no student_id field
                        FindIterable<Document> fmt1Docs = paymentsCol.find(
                            Filters.and(
                                Filters.eq("enrollment_id", enrId),
                                Filters.exists("student_id", false)
                            )
                        ).sort(new Document("payment_date", -1));

                        for (Document pay : fmt1Docs) {
                            // Parse amount safely
                            double amount = parseAmount(pay.get("amount"));
                            
                            // Skip invalid records with zero amount
                            if (amount <= 0) continue;

                            // Resolve batch name
                            String batchLabel = "Unknown Batch";
                            Object batchIdObj = enrollment.get("batch_id");
                            if (batchIdObj != null) {
                                Document batch = batchesCol.find(
                                    new Document("_id", batchIdObj)
                                ).first();
                                if (batch != null)
                                    batchLabel = batch.getString("batch_name");
                            }

                            String mode   = pay.getString("mode");
                            String status = pay.getString("status");
                            Date   date   = pay.getDate("payment_date");

                            // Skip if no valid date
                            if (date == null) continue;

                            publish(new Object[]{
                                sdf.format(date),
                                batchLabel,
                                String.format("₹%.0f", amount),
                                mode   != null ? mode.toUpperCase()   : "—",
                                status != null ? status.toUpperCase() : "—"
                            });
                        }
                    }

                    // ────────────────────────────────────────────────────────
                    // FORMAT 2: direct student_id payments
                    // These have "student_id" field and NO "enrollment_id"
                    // ────────────────────────────────────────────────────────
                    FindIterable<Document> fmt2Docs = paymentsCol.find(
                        Filters.and(
                            Filters.eq("student_id", studentId),
                            Filters.exists("enrollment_id", false)
                        )
                    ).sort(new Document("payment_date", -1));

                    for (Document pay : fmt2Docs) {
                        // Parse amount from "amount_paid" field
                        double amount = parseAmount(pay.get("amount_paid"));

                        // Skip invalid/incomplete records with zero amount
                        if (amount <= 0) continue;

                        // Resolve subject name
                        // subject_id is stored as String "5" but subjects._id
                        // is integer 5 — must parse before querying
                        String subjectIdStr = pay.getString("subject_id");
                        if (subjectIdStr == null || subjectIdStr.trim().isEmpty()) {
                            // No subject — this is an incomplete record, skip
                            continue;
                        }

                        String subjectLabel;
                        try {
                            int subjectIdInt = Integer.parseInt(
                                subjectIdStr.trim());
                            Document subject = subjectsCol.find(
                                new Document("_id", subjectIdInt)
                            ).first();
                            subjectLabel = (subject != null)
                                ? subject.getString("subject_name")
                                : "Subject " + subjectIdStr;
                        } catch (NumberFormatException ex) {
                            subjectLabel = "Subject " + subjectIdStr;
                        }

                        // Build display label with month
                        int month = pay.getInteger("month", 0);
                        String monthLabel = (month >= 1 && month <= 12)
                            ? new SimpleDateFormat("MMM yyyy").format(
                                new GregorianCalendar(2026, month - 1, 1)
                                    .getTime())
                            : "";
                        String batchLabel = subjectLabel
                            + (monthLabel.isEmpty() ? "" : " — " + monthLabel);

                        // Use payment_date; fallback to created_at only if
                        // payment_date is null — never show created_at as date
                        Date date = pay.getDate("payment_date");
                        if (date == null) date = pay.getDate("created_at");
                        if (date == null) continue; // skip undated records

                        String mode = pay.getString("payment_mode");

                        publish(new Object[]{
                            sdf.format(date),
                            batchLabel,
                            String.format("₹%.0f", amount),
                            mode != null ? mode.toUpperCase() : "—",
                            "PAID"
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) historyModel.addRow(row);
            }

            @Override
            protected void done() {
                if (historyModel.getRowCount() == 0) {
                    historyModel.addRow(new Object[]{
                        "—", "No payment records found", "—", "—", "—"
                    });
                }
            }
        }.execute();
    }

    private double parseAmount(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Double)  return (Double) value;
        if (value instanceof Long)    return ((Long) value).doubleValue();
        if (value instanceof org.bson.types.Decimal128)
            return ((org.bson.types.Decimal128) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (Exception e) { return 0; }
    }

    private void generateAndDownloadReceipt(String date, String batch,
            String amount, String method, String status) {
        
        String studentName = currentStudent != null ? 
                             currentStudent.getName() : "Student";
        
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(
            "Receipt_" + studentName.replace(" ", "_") 
            + "_" + date.replace("-", "") + ".pdf"
        ));
        
        int result = chooser.showSaveDialog(ParentFeesPanel.this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        
        String filePath = chooser.getSelectedFile().getAbsolutePath();
        if (!filePath.endsWith(".pdf")) filePath += ".pdf";
        
        try {
            com.itextpdf.text.Document pdf = 
                new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(
                pdf, new java.io.FileOutputStream(filePath));
            pdf.open();
            
            // Title
            com.itextpdf.text.Font titleFont = 
                com.itextpdf.text.FontFactory.getFont(
                    com.itextpdf.text.FontFactory.HELVETICA_BOLD, 20);
            pdf.add(new com.itextpdf.text.Paragraph(
                "MRK TUITION — PAYMENT RECEIPT", titleFont));
            pdf.add(com.itextpdf.text.Chunk.NEWLINE);
            pdf.add(new com.itextpdf.text.Paragraph(
                "─────────────────────────────────────────"));
            pdf.add(com.itextpdf.text.Chunk.NEWLINE);
            
            // Receipt details
            com.itextpdf.text.Font labelFont = 
                com.itextpdf.text.FontFactory.getFont(
                    com.itextpdf.text.FontFactory.HELVETICA_BOLD, 13);
            com.itextpdf.text.Font valueFont = 
                com.itextpdf.text.FontFactory.getFont(
                    com.itextpdf.text.FontFactory.HELVETICA, 13);
            
            pdf.add(new com.itextpdf.text.Paragraph(
                "Student Name : " + studentName, labelFont));
            pdf.add(new com.itextpdf.text.Paragraph(
                "Batch / Subject : " + batch, valueFont));
            pdf.add(new com.itextpdf.text.Paragraph(
                "Amount Paid  : " + amount, valueFont));
            pdf.add(new com.itextpdf.text.Paragraph(
                "Payment Date : " + date, valueFont));
            pdf.add(new com.itextpdf.text.Paragraph(
                "Payment Mode : " + method, valueFont));
            pdf.add(new com.itextpdf.text.Paragraph(
                "Status       : " + status, valueFont));
            pdf.add(com.itextpdf.text.Chunk.NEWLINE);
            pdf.add(new com.itextpdf.text.Paragraph(
                "─────────────────────────────────────────"));
            pdf.add(new com.itextpdf.text.Paragraph(
                "Thank you for your payment. — MRK Tuition Center",
                valueFont));
            
            pdf.close();
            
            JOptionPane.showMessageDialog(
                ParentFeesPanel.this,
                "Receipt saved successfully!\n" + filePath,
                "Receipt Downloaded",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                ParentFeesPanel.this,
                "Failed to generate receipt: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
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

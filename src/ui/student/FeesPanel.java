package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.Fee;
import model.Payment;
import dao.FeesDAO;
import util.SessionManager;

public class FeesPanel extends JPanel {

    private DefaultTableModel model;
    private JPanel statsPanel;
    private JTable payTable;

    public FeesPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Fees & Payments");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        JLabel subtitle = new JLabel("View your outstanding balances and payment history");
        subtitle.setForeground(Color.GRAY);
        
        header.add(title);
        header.add(subtitle);
        
        JPanel feesPanel = new JPanel(new BorderLayout());
        feesPanel.add(header, BorderLayout.NORTH);

        statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(Color.WHITE);
        feesPanel.add(statsPanel, BorderLayout.CENTER);
        
        add(feesPanel, BorderLayout.NORTH);
        
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(Color.WHITE);
        historyPanel.setBorder(new EmptyBorder(30,0,0,0));
        
        JLabel htitle = new JLabel("Payment History");
        htitle.setFont(new Font("Arial", Font.BOLD, 18));
        htitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        historyPanel.add(htitle, BorderLayout.NORTH);

        String columns[] = {"Receipt No", "Date", "Amount Paid", "Mode"};
        model = new DefaultTableModel(columns, 0);
        payTable = new JTable(model);
        payTable.setRowHeight(40);
        payTable.setIntercellSpacing(new Dimension(0, 0));
        payTable.setShowGrid(false);
        payTable.setShowHorizontalLines(true);
        payTable.setGridColor(new Color(230, 230, 230));

        loadDataAsync();

        JScrollPane scrollPane = new JScrollPane(payTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(historyPanel, BorderLayout.CENTER);
    }

    private JPanel createModernStatCard(String title, String value, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 22));
        
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    private void loadDataAsync() {
        statsPanel.removeAll();
        statsPanel.add(createModernStatCard("Loading...", "", new Color(100, 150, 255)));
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "", "", ""});
        
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                if (userId == null) return new Object[]{null, new java.util.ArrayList<Payment>()};
                Fee f = new FeesDAO().getFeeSummaryForStudent(userId);
                List<Payment> history = new FeesDAO().getPaymentsForStudent(userId);
                return new Object[]{f, history};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    Fee f = (Fee) result[0];
                    List<Payment> history = (List<Payment>) result[1];

                    statsPanel.removeAll();
                    double total = f != null ? f.getTotalAmount() : 0.0;
                    double paid = f != null ? f.getPaidAmount() : 0.0;
                    double pending = total - paid;
                    String status = f != null ? f.getStatus() : "N/A";
            
                    statsPanel.add(createModernStatCard("Total Fees", "Rs. " + total, new Color(100, 150, 255)));
                    statsPanel.add(createModernStatCard("Amount Paid", "Rs. " + paid, new Color(100, 200, 150)));
                    statsPanel.add(createModernStatCard("Pending", "Rs. " + pending, new Color(255, 100, 100)));
                    statsPanel.add(createModernStatCard("Status", status, new Color(180, 100, 255)));
                    statsPanel.revalidate();
                    statsPanel.repaint();

                    model.setRowCount(0);
                    if (history != null && !history.isEmpty()) {
                        for (Payment p : history) {
                            String dStr = p.getPaymentDate() != null ? p.getPaymentDate().toString().substring(0, 10) : "";
                            model.addRow(new Object[]{
                                p.getReceiptNo() != null ? p.getReceiptNo() : "N/A",
                                dStr,
                                "Rs. " + p.getAmountPaid(),
                                p.getPaymentMode()
                            });
                        }
                    } else {
                        model.addRow(new Object[]{"No Data Available", "", "", ""});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    model.setRowCount(0);
                    model.addRow(new Object[]{"Error loading data", "", "", ""});
                }
            }
        }.execute();
    }
}

package ui.parent;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.bson.Document;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import db.DBConnection;
import util.SessionManager;
import util.ThemeManager;

/**
 * Professional Notification Panel for Parents
 * Displays only notification messages and dates in a clean table format.
 */
public class ParentNotificationPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    public ParentNotificationPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initTable();
        loadNotifications();
    }

    private void initHeader() {
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(ThemeManager.BG);
        
        JLabel title = new JLabel("Notifications");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        
        JLabel subtitle = new JLabel("All your alerts and messages from the tuition center");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(ThemeManager.SUB_TEXT);
        
        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Message", "Date"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        
        table = new JTable(model);
        table.setRowHeight(50);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(245, 247, 250));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Message column width
        table.getColumnModel().getColumn(0).setPreferredWidth(800);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245)));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    private String resolveParentId(String userId) {
        MongoDatabase db = DBConnection.getDatabase();
        
        // Try parents collection first
        Document parentDoc = db.getCollection("parents").find(
            Filters.eq("user_id", userId)
        ).first();
        if (parentDoc != null) {
            return parentDoc.getString("user_id"); 
        }
        
        // For the main parents (U31–U50), find their matching P0XX id
        Document userDoc = db.getCollection("users").find(
            Filters.eq("_id", userId)
        ).first();
        if (userDoc != null) {
            java.util.List<String> phones = userDoc.getList("phones", String.class);
            if (phones != null && !phones.isEmpty()) {
                String phone = phones.get(0);
                Document studentDoc = db.getCollection("students").find(
                    Filters.eq("parent.phone", phone)
                ).first();
                if (studentDoc != null) {
                    Document parentEmbed = (Document) studentDoc.get("parent");
                    if (parentEmbed != null) {
                        return parentEmbed.getString("parent_id");
                    }
                }
            }
        }
        return userId;
    }

    private void loadNotifications() {
        String userId = SessionManager.getInstance().getUserId();
        if (userId == null) return;

        new SwingWorker<Void, Object[]>() {
            @Override protected Void doInBackground() throws Exception {
                MongoDatabase db = DBConnection.getDatabase();
                if (db == null) return null;
                
                String parentId = resolveParentId(userId);
                System.out.println("[Notifications] userId=" + userId + " resolved parentId=" + parentId);
                
                MongoCollection<Document> notifCol = db.getCollection("notifications");
                
                FindIterable<Document> list = notifCol.find(Filters.eq("parent_id", parentId))
                                                   .sort(Sorts.descending("date"));
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
                boolean hasResults = false;
                
                for (Document doc : list) {
                    hasResults = true;
                    String title = doc.getString("title");
                    String msg = doc.getString("message");
                    Date date = doc.getDate("date");
                    
                    String displayMsg = (title != null ? title + "  —  " : "") + (msg != null ? msg : "");
                    String displayDate = date != null ? sdf.format(date) : "-";
                    
                    publish(new Object[]{ displayMsg, displayDate });
                    
                    notifCol.updateOne(
                        Filters.eq("_id", doc.getObjectId("_id")),
                        new Document("$set", new Document("is_read", true))
                    );
                }
                
                if (!hasResults) {
                    publish(new Object[]{ 
                        "No notifications yet. Notifications will appear here when fees are paid, marks are published, or attendance is updated.", 
                        "-" 
                    });
                }
                return null;
            }

            @Override protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    model.addRow(row);
                }
            }
        }.execute();
    }
}

package ui.common;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import org.bson.Document;
import service.NotificationService;

/**
 * Notification Inbox — a non-modal JDialog opened by clicking the bell icon.
 * Shows last 20 notifications for the current user as scrollable cards.
 * Opening the inbox automatically marks all as read and resets the badge.
 *
 * Usage:
 *   NotificationInboxPanel.showInbox(parentWindow, "ADMIN", "ADMIN", badgeLabel);
 *   NotificationInboxPanel.showInbox(parentWindow, "TEACHER", "T001", badgeLabel);
 *   NotificationInboxPanel.showInbox(parentWindow, "STUDENT", "S001", badgeLabel);
 */
public class NotificationInboxPanel {

    private static final Color BG_DARK     = new Color(10, 27, 63);
    private static final Color BG_PAGE     = new Color(245, 247, 250);
    private static final Color BG_UNREAD   = new Color(235, 244, 255);
    private static final Color BG_READ     = Color.WHITE;
    private static final Color BORDER_C    = new Color(220, 225, 235);
    private static final Color ACCENT_BLUE = new Color(0, 120, 215);
    private static final Color TEXT_DARK   = new Color(26, 43, 71);
    private static final Color TEXT_GREY   = new Color(80, 90, 110);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MM-yyyy hh:mm a");

    /**
     * Open the notification inbox dialog.
     *
     * @param owner         the parent window (can be null)
     * @param recipientType "ADMIN" | "TEACHER" | "STUDENT" | "PARENT"
     * @param recipientId   e.g. "ADMIN", "T001", "S001"
     * @param badgeLabel    the red badge label — will be hidden after opening (pass null if not needed)
     */
    public static void showInbox(Window owner,
                                  String recipientType,
                                  String recipientId,
                                  JLabel badgeLabel) {

        JDialog dialog = new JDialog(owner, "Notifications", Dialog.ModalityType.MODELESS);
        dialog.setSize(440, 580);
        dialog.setLocationRelativeTo(owner);
        dialog.setLayout(new BorderLayout());
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_C));

        // ── Header bar ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel titleLbl = new JLabel("🔔  Notifications");
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));

        JButton markAllBtn = new JButton("Mark all read");
        markAllBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        markAllBtn.setForeground(new Color(173, 210, 255));
        markAllBtn.setContentAreaFilled(false);
        markAllBtn.setBorderPainted(false);
        markAllBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        header.add(titleLbl,   BorderLayout.WEST);
        header.add(markAllBtn, BorderLayout.EAST);

        // ── Notification list ──────────────────────────────────────────────────
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_PAGE);
        listPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        List<Document> notifs = NotificationService.getInstance()
            .getRecentNotifications(recipientType, recipientId, 20);

        if (notifs.isEmpty()) {
            JLabel empty = new JLabel("No notifications yet.", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 13));
            empty.setForeground(Color.GRAY);
            empty.setBorder(new EmptyBorder(50, 0, 0, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(empty);
        } else {
            for (Document n : notifs) {
                listPanel.add(buildCard(n));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(14);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        footer.setBackground(new Color(248, 250, 253));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

        // Mark all read immediately on open + hide badge
        NotificationService.getInstance().markAllAsRead(recipientType, recipientId);
        if (badgeLabel != null) {
            SwingUtilities.invokeLater(() -> badgeLabel.setVisible(false));
        }

        // "Mark all read" button re-marks and reopens fresh
        markAllBtn.addActionListener(e -> {
            NotificationService.getInstance().markAllAsRead(recipientType, recipientId);
            if (badgeLabel != null) badgeLabel.setVisible(false);
            dialog.dispose();
            showInbox(owner, recipientType, recipientId, badgeLabel);
        });

        dialog.setVisible(true);
    }

    // ── Build a single notification card ────────────────────────────────────────

    private static JPanel buildCard(Document n) {
        boolean unread = !Boolean.TRUE.equals(n.getBoolean("is_read"));
        String notifId = n.getString("_id");

        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setBackground(unread ? BG_UNREAD : BG_READ);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Unread: left blue border stripe
        if (unread) {
            card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 3, 1, 0, ACCENT_BLUE),
                new EmptyBorder(10, 11, 10, 14)));
        } else {
            card.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_C),
                new EmptyBorder(10, 14, 10, 14)));
        }

        // Type badge (coloured dot)
        String type   = n.getString("type");
        Color dotColor = typeColor(type);
        JLabel dotLbl  = new JLabel("●");
        dotLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        dotLbl.setForeground(dotColor);

        // Title
        JLabel titleLbl = new JLabel(n.getString("title") != null ? n.getString("title") : "");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLbl.setForeground(TEXT_DARK);

        // Title row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titleRow.setOpaque(false);
        titleRow.add(dotLbl);
        titleRow.add(titleLbl);

        // Message (truncated)
        String msg = n.getString("message");
        if (msg != null && msg.length() > 130) msg = msg.substring(0, 127) + "…";
        JLabel msgLbl = new JLabel("<html><body style='width:360px'>" + escHtml(msg) + "</body></html>");
        msgLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        msgLbl.setForeground(TEXT_GREY);

        // Date
        java.util.Date d = n.getDate("date");
        JLabel dateLbl = new JLabel(d != null ? SDF.format(d) : "");
        dateLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        dateLbl.setForeground(Color.GRAY);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(titleRow);
        text.add(Box.createVerticalStrut(2));
        text.add(msgLbl);
        text.add(Box.createVerticalStrut(3));
        text.add(dateLbl);

        card.add(text, BorderLayout.CENTER);

        // Hover + click interactions
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final boolean[] isUnread = {unread};
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (isUnread[0]) {
                    NotificationService.getInstance().markAsRead(notifId);
                    card.setBackground(BG_READ);
                    card.setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(0, 0, 1, 0, BORDER_C),
                        new EmptyBorder(10, 14, 10, 14)));
                    isUnread[0] = false;
                    card.repaint();
                }
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(225, 237, 252));
                card.repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(isUnread[0] ? BG_UNREAD : BG_READ);
                card.repaint();
            }
        });

        return card;
    }

    // ── Colour per notification type ────────────────────────────────────────────
    private static Color typeColor(String type) {
        if (type == null) return Color.GRAY;
        return switch (type) {
            case "FEE_PAID"            -> new Color(22, 163, 74);
            case "FEE_PARTIAL"         -> new Color(245, 158, 11);
            case "FEE_OVERDUE"         -> new Color(220, 38, 38);
            case "ENROLLMENT_CONFIRMED"-> new Color(59, 130, 246);
            case "ATTENDANCE_ABSENT",
                 "ATTENDANCE_ALERT"    -> new Color(234, 88, 12);
            case "TEST_RESULT",
                 "MARKS_UPDATED"       -> new Color(139, 92, 246);
            case "TEST_RESULT_PENDING" -> new Color(156, 163, 175);
            case "SALARY_PROCESSED"    -> new Color(16, 185, 129);
            case "SALARY_DEDUCTION"    -> new Color(239, 68, 68);
            case "SALARY_BONUS"        -> new Color(251, 191, 36);
            case "BATCH_ASSIGNED"      -> new Color(99, 102, 241);
            default                    -> new Color(107, 114, 128);
        };
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

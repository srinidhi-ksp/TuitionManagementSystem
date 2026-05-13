package service;

import java.util.Date;
import org.bson.Document;

/**
 * Builder-style model for a single notification document.
 * Matches the schema stored in the 'notifications' collection
 * in tuitionManagementDB.
 *
 * Usage:
 *   NotificationService.getInstance().push(
 *       new NotificationDocument(ROLE_STUDENT, "S001", FEE_PAID, "Fee Received", "Your fee of ₹500...")
 *           .studentId("S001").studentName("Arjun").batchId("B-001")
 *   );
 */
public class NotificationDocument {

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final String id;
    private final String recipientType;   // STUDENT | PARENT | TEACHER | ADMIN
    private final String recipientId;
    private final String type;
    private final String title;
    private final String message;

    // Optional metadata — set via fluent setters
    private String studentId;
    private String studentName;
    private String batchId;
    private String subject;
    private String month;
    private String year;
    private Date   date;
    private boolean isRead = false;

    // ── Constructor ────────────────────────────────────────────────────────────
    public NotificationDocument(String recipientType, String recipientId,
                                 String type, String title, String message) {
        this.id            = NotificationService.generateId("NTF");
        this.recipientType = recipientType;
        this.recipientId   = recipientId;
        this.type          = type;
        this.title         = title;
        this.message       = message;
        this.date          = new Date();
    }

    // ── Fluent setters (return this for chaining) ──────────────────────────────
    public NotificationDocument studentId(String sid)   { this.studentId   = sid;  return this; }
    public NotificationDocument studentName(String sn)  { this.studentName = sn;   return this; }
    public NotificationDocument batchId(String bid)     { this.batchId     = bid;  return this; }
    public NotificationDocument subject(String sub)     { this.subject     = sub;  return this; }
    public NotificationDocument month(String m)         { this.month       = m;    return this; }
    public NotificationDocument year(String y)          { this.year        = y;    return this; }
    public NotificationDocument date(Date d)            { this.date        = d;    return this; }

    // ── Convert to BSON ────────────────────────────────────────────────────────
    public Document toBson() {
        Document doc = new Document("_id",            id)
            .append("recipient_type", recipientType)
            .append("recipient_id",   recipientId)
            .append("type",           type)
            .append("title",          title)
            .append("message",        message)
            .append("is_read",        isRead)
            .append("date",           date != null ? date : new Date());

        if (studentId   != null) doc.append("student_id",   studentId);
        if (studentName != null) doc.append("student_name", studentName);
        if (batchId     != null) doc.append("batch_id",     batchId);
        if (subject     != null) doc.append("subject",      subject);
        if (month       != null) doc.append("month",        month);
        if (year        != null) doc.append("year",         year);

        return doc;
    }

    // ── Getters (for potential future use) ────────────────────────────────────
    public String getId()            { return id; }
    public String getRecipientType() { return recipientType; }
    public String getRecipientId()   { return recipientId; }
    public String getType()          { return type; }
    public String getTitle()         { return title; }
    public String getMessage()       { return message; }
}

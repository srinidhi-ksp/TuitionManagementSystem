package dao;

import java.util.Date;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import model.Payment;

/**
 * Payment DAO - Handles all payment-related database operations
 */
public class PaymentDAO {
    private MongoCollection<Document> paymentCollection;

    public PaymentDAO() {
        try {
            MongoDatabase database = DBConnection.getDatabase();
            if (database != null) {
                paymentCollection = database.getCollection("payments");
                System.out.println("[PaymentDAO] ✅ Connected to 'payments' collection");
            } else {
                System.err.println("[PaymentDAO] ❌ Database connection failed!");
            }
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get payment record for a specific student and batch for a given month
     */
    public Payment getPaymentForBatch(String studentId, int batchId, String month) {
        if (paymentCollection == null) return null;

        try {
            Document doc = paymentCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("batch_id", batchId),
                    Filters.eq("month", month)
                )
            ).first();

            if (doc != null) {
                return db.DocumentMapper.documentToPayment(doc);
            }

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error checking payment status: " + e.getMessage());
        }
        return null;
    }

    /**
     * Insert a new payment record
     */
    public boolean insertPayment(Payment payment) {
        if (paymentCollection == null) {
            System.err.println("[PaymentDAO] ❌ Payment collection is null!");
            return false;
        }

        try {
            Document doc = new Document()
                .append("student_id", payment.getStudentId())
                .append("batch_id", payment.getBatchId())
                .append("amount", payment.getAmountPaid())
                .append("payment_mode", payment.getPaymentMode())
                .append("payment_date", payment.getPaymentDate())
                .append("month", payment.getMonthStr())
                .append("status", payment.getStatus() != null ? payment.getStatus() : "PAID")
                .append("created_at", new Date());

            paymentCollection.insertOne(doc);
            System.out.println("[PaymentDAO] ✅ Payment inserted successfully");
            return true;

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error inserting payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Payment getPayment(String studentId, int batchId) {
        if (paymentCollection == null) return null;

        try {
            Document doc = paymentCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("batch_id", batchId)
                )
            ).first();

            if (doc != null) {
                return db.DocumentMapper.documentToPayment(doc);
            }

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error fetching payment: " + e.getMessage());
        }

        return null;
    }

    /**
     * Delete a payment record (for undoing payments)
     */
    public boolean deletePayment(String studentId, int batchId) {
        if (paymentCollection == null) return false;

        try {
            long deletedCount = paymentCollection.deleteOne(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("batch_id", batchId)
                )
            ).getDeletedCount();

            return deletedCount > 0;

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error deleting payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a student has paid for a specific batch in the current month
     */
    public boolean isBatchPaid(String studentId, int batchId) {
        if (paymentCollection == null) return false;
        
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date());
        
        try {
            Document doc = paymentCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("batch_id", batchId),
                    Filters.eq("month", currentMonth),
                    Filters.eq("status", "PAID")
                )
            ).first();

            return doc != null;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error checking batch payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a student has paid for a specific subject (LEGACY)
     */
    public boolean isSubjectPaid(String studentId, String subjectId) {
        if (paymentCollection == null) return false;

        try {
            Document doc = paymentCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("subject_id", subjectId),
                    Filters.eq("status", "PAID")
                )
            ).first();

            return doc != null;

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error checking subject payment: " + e.getMessage());
            return false;
        }
    }
    /**
     * Get recent payments for a student
     */
    public java.util.List<Payment> getRecentPayments(String studentId, int limit) {
        java.util.List<Payment> list = new java.util.ArrayList<>();
        if (paymentCollection == null) return list;
        try {
            com.mongodb.client.MongoCursor<Document> cursor = paymentCollection.find(com.mongodb.client.model.Filters.eq("student_id", studentId))
                .sort(new Document("payment_date", -1))
                .limit(limit)
                .iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Payment p = new Payment();
                p.setStudentId(doc.getString("student_id"));
                p.setSubjectId(doc.getString("subject_id"));
                p.setAmountPaid(doc.getDouble("amount_paid"));
                p.setPaymentMode(doc.getString("payment_mode"));
                p.setPaymentDate(doc.getDate("payment_date"));
                p.setMonth(doc.getInteger("month"));
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

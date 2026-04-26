package dao;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import db.DBConnection;
import model.Payment;
import java.util.Date;

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
     * Check if a subject is paid for a student
     */
    public boolean isSubjectPaid(String studentId, String subjectId) {
        if (paymentCollection == null) return false;

        try {
            Document payment = paymentCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("subject_id", subjectId)
                )
            ).first();

            return payment != null;

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error checking payment status: " + e.getMessage());
            return false;
        }
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
                .append("subject_id", payment.getSubjectId())
                .append("amount_paid", payment.getAmountPaid())
                .append("payment_mode", payment.getPaymentMode())
                .append("payment_date", payment.getPaymentDate())
                .append("month", payment.getMonth())
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

    /**
     * Get payment record for a specific student-subject combination
     */
    public Payment getPayment(String studentId, String subjectId) {
        if (paymentCollection == null) return null;

        try {
            Document doc = paymentCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("subject_id", subjectId)
                )
            ).first();

            if (doc != null) {
                Payment payment = new Payment();
                payment.setStudentId(doc.getString("student_id"));
                payment.setSubjectId(doc.getString("subject_id"));
                payment.setAmountPaid(doc.getDouble("amount_paid"));
                payment.setPaymentMode(doc.getString("payment_mode"));
                payment.setPaymentDate(doc.getDate("payment_date"));
                payment.setMonth(doc.getString("month"));
                return payment;
            }

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error fetching payment: " + e.getMessage());
        }

        return null;
    }

    /**
     * Delete a payment record (for undoing payments)
     */
    public boolean deletePayment(String studentId, String subjectId) {
        if (paymentCollection == null) return false;

        try {
            long deletedCount = paymentCollection.deleteOne(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("subject_id", subjectId)
                )
            ).getDeletedCount();

            return deletedCount > 0;

        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error deleting payment: " + e.getMessage());
            return false;
        }
    }
}

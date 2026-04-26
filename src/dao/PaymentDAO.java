package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import model.Payment;
import db.DBConnection;
import db.DocumentMapper;

/**
 * PaymentDAO - Handles subject-wise payment operations
 * Schema: student_id, subject_id, amount_paid, payment_mode, payment_date, month
 */
public class PaymentDAO {
    private MongoCollection<Document> paymentsCollection;

    public PaymentDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            paymentsCollection = database.getCollection("payments");
        }
    }

    /**
     * Insert a new payment record
     */
    public boolean insertPayment(Payment payment) {
        if (paymentsCollection == null) return false;
        try {
            Document doc = DocumentMapper.paymentToDocument(payment);
            paymentsCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if a subject is paid for a student
     * Returns true if any payment exists for this student-subject combination
     */
    public boolean isSubjectPaid(String studentId, int subjectId) {
        if (paymentsCollection == null) return false;
        try {
            Document doc = paymentsCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("subject_id", subjectId)
                )
            ).first();
            return doc != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all payments for a student
     */
    public List<Payment> getPaymentsByStudentId(String studentId) {
        List<Payment> list = new ArrayList<>();
        if (paymentsCollection == null) return list;

        try (MongoCursor<Document> cursor = paymentsCollection.find(
                Filters.eq("student_id", studentId)
            ).sort(Sorts.descending("payment_date")).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Payment p = DocumentMapper.documentToPayment(doc);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Get payment for a specific student-subject combination
     */
    public Payment getPaymentByStudentAndSubject(String studentId, int subjectId) {
        if (paymentsCollection == null) return null;
        try {
            Document doc = paymentsCollection.find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.eq("subject_id", subjectId)
                )
            ).first();
            return DocumentMapper.documentToPayment(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get all payments for a specific subject
     * (Admin view - all students who paid for a subject)
     */
    public List<Payment> getPaymentsBySubjectId(int subjectId) {
        List<Payment> list = new ArrayList<>();
        if (paymentsCollection == null) return list;

        try (MongoCursor<Document> cursor = paymentsCollection.find(
                Filters.eq("subject_id", subjectId)
            ).sort(Sorts.descending("payment_date")).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Payment p = DocumentMapper.documentToPayment(doc);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Get all payments
     */
    public List<Payment> getAllPayments() {
        List<Payment> list = new ArrayList<>();
        if (paymentsCollection == null) return list;

        try (MongoCursor<Document> cursor = paymentsCollection.find()
            .sort(Sorts.descending("payment_date")).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Payment p = DocumentMapper.documentToPayment(doc);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Delete a payment by ID
     */
    public boolean deletePayment(int paymentId) {
        if (paymentsCollection == null) return false;
        try {
            long deletedCount = paymentsCollection.deleteOne(Filters.eq("_id", paymentId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Update payment
     */
    public boolean updatePayment(Payment payment) {
        if (paymentsCollection == null) return false;
        try {
            Document doc = DocumentMapper.paymentToDocument(payment);
            long matched = paymentsCollection.replaceOne(Filters.eq("_id", payment.getPaymentId()), doc).getMatchedCount();
            return matched > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

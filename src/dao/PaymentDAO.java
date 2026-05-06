package dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
     * Insert a new payment record.
     * Standard schema: student_id, batch_id, amount, method, status, date, month
     * status values: SUCCESS | REQUESTED
     */
    public boolean insertPayment(Payment payment) {
        if (paymentCollection == null) return false;

        try {
            String currentMonth = new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date());
            String status = payment.getStatus() != null ? payment.getStatus() : "SUCCESS";

            System.out.println("[PaymentDAO] Inserting payment: student=" + payment.getStudentId()
                + " batch=" + payment.getBatchId()
                + " amount=" + payment.getAmountPaid()
                + " method=" + payment.getPaymentMode()
                + " status=" + status);

            // STEP 1 — DUPLICATE GUARD: block double-payment for same batch+month with SUCCESS
            if ("SUCCESS".equalsIgnoreCase(status)) {
                Document existing = paymentCollection.find(
                    Filters.and(
                        Filters.eq("student_id", payment.getStudentId()),
                        Filters.eq("batch_id", payment.getBatchId()),
                        Filters.eq("month", currentMonth),
                        Filters.eq("status", "SUCCESS")
                    )
                ).first();
                if (existing != null) {
                    System.out.println("[PaymentDAO] ⚠️ Already paid for batch " + payment.getBatchId() + " this month.");
                    return false; // caller should show "Already Paid" message
                }
            }

            // STEP 2 — INSERT with standard field names: amount, method, date
            Document doc = new Document()
                .append("student_id", payment.getStudentId())
                .append("batch_id", payment.getBatchId())
                .append("amount", payment.getAmountPaid())
                .append("method", payment.getPaymentMode())
                .append("status", status)
                .append("date", new Date())
                .append("month", currentMonth);

            paymentCollection.insertOne(doc);
            System.out.println("[PaymentDAO] ✅ Payment inserted successfully.");

            // STEP 3 — Sync fees + student totals only for SUCCESS payments
            if ("SUCCESS".equalsIgnoreCase(status)) {
                syncPaymentWithFeesAndStudent(payment.getStudentId(), payment.getBatchId(), payment.getAmountPaid());
                triggerFeePaidNotification(payment.getStudentId(), payment.getBatchId(), payment.getAmountPaid(), payment.getPaymentMode());
            }

            return true;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] ❌ insertPayment error: " + e.getMessage());
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
     * Check if a student has a SUCCESS payment for a specific batch in the current month
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
                    Filters.or(
                        Filters.eq("status", "SUCCESS"),
                        Filters.eq("status", "PAID")
                    )
                )
            ).first();
            return doc != null;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error checking batch payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get raw payment summary for a student+batch directly from MongoDB.
     * Returns a map with keys: "totalPaid" (Double), "status" (String), "method" (String), "month" (String)
     * Status values: "SUCCESS", "REQUESTED", "UNPAID"
     * This bypasses DocumentMapper entirely — same approach as FeeAnalyticsDAO.
     */
    public java.util.Map<String, Object> getBatchPaymentSummary(String studentId, int batchId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalPaid", 0.0);
        result.put("status", "UNPAID");
        result.put("method", "—");
        result.put("month", "");

        if (paymentCollection == null) return result;

        try {
            // Fetch ALL payment docs for this student+batch (any status, any month)
            java.util.List<org.bson.Document> docs = new java.util.ArrayList<>();
            paymentCollection.find(
                com.mongodb.client.model.Filters.and(
                    com.mongodb.client.model.Filters.eq("student_id", studentId),
                    com.mongodb.client.model.Filters.eq("batch_id", batchId)
                )
            ).into(docs);

            if (docs.isEmpty()) return result; // UNPAID, no records

            double totalPaid = 0.0;
            String latestStatus = "UNPAID";
            String latestMethod = "—";
            String latestMonth = "";
            java.util.Date latestDate = null;

            for (org.bson.Document doc : docs) {
                String status = doc.getString("status");
                if (status == null) status = "";

                // Read amount — try "amount" first, then "amount_paid" as legacy fallback
                Object amtObj = doc.get("amount");
                if (amtObj == null) amtObj = doc.get("amount_paid");
                double amt = (amtObj instanceof Number) ? ((Number) amtObj).doubleValue() : 0.0;

                // Only count SUCCESS or PAID toward totalPaid
                if ("SUCCESS".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
                    totalPaid += amt;
                }

                // Track the most recent document for method/status display
                Object dateObj = doc.get("date");
                if (dateObj == null) dateObj = doc.get("payment_date");
                java.util.Date docDate = (dateObj instanceof java.util.Date) ? (java.util.Date) dateObj : null;

                if (latestDate == null || (docDate != null && docDate.after(latestDate))) {
                    latestDate = docDate;
                    latestStatus = status;
                    Object methodObj = doc.get("method");
                    if (methodObj == null) methodObj = doc.get("payment_mode");
                    latestMethod = (methodObj != null) ? methodObj.toString() : "—";
                    String m = doc.getString("month");
                    latestMonth = (m != null) ? m : "";
                }
            }

            result.put("totalPaid", totalPaid);
            result.put("method", latestMethod);
            result.put("month", latestMonth);

            // Determine display status
            if (totalPaid > 0) {
                result.put("status", "SUCCESS"); // Will be resolved to PAID/PARTIAL by caller
            } else if ("REQUESTED".equalsIgnoreCase(latestStatus) || "CASH_REQUEST".equalsIgnoreCase(latestMethod)) {
                result.put("status", "REQUESTED");
            } else {
                result.put("status", "UNPAID");
            }

        } catch (Exception e) {
            System.err.println("[PaymentDAO] getBatchPaymentSummary error: " + e.getMessage());
        }

        return result;
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
     * Get recent payments for a student (reads standard schema: amount/method/date)
     */
    public java.util.List<Payment> getRecentPayments(String studentId, int limit) {
        java.util.List<Payment> list = new java.util.ArrayList<>();
        if (paymentCollection == null) return list;
        try {
            com.mongodb.client.MongoCursor<Document> cursor = paymentCollection
                .find(Filters.eq("student_id", studentId))
                .sort(new Document("date", -1))
                .limit(limit)
                .iterator();
            while (cursor.hasNext()) {
                list.add(db.DocumentMapper.documentToPayment(cursor.next()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Admin approval: updates REQUESTED → SUCCESS (does NOT insert a new document).
     * Also updates method to CASH and stamps the approval date.
     */
    public boolean markBatchAsPaid(String studentId, int batchId, double amount) {
        if (paymentCollection == null) return false;
        try {
            long matched = paymentCollection.updateMany(
                com.mongodb.client.model.Filters.and(
                    com.mongodb.client.model.Filters.eq("student_id", studentId),
                    com.mongodb.client.model.Filters.eq("batch_id", batchId),
                    com.mongodb.client.model.Filters.or(
                        com.mongodb.client.model.Filters.eq("status", "REQUESTED"),
                        com.mongodb.client.model.Filters.eq("method", "CASH_REQUEST")
                    )
                ),
                com.mongodb.client.model.Updates.combine(
                    com.mongodb.client.model.Updates.set("status", "SUCCESS"),
                    com.mongodb.client.model.Updates.set("method", "CASH"),
                    com.mongodb.client.model.Updates.set("date", new Date()),
                    com.mongodb.client.model.Updates.set("amount", amount)
                )
            ).getMatchedCount();

            System.out.println("[PaymentDAO] markBatchAsPaid: matched=" + matched + " for student=" + studentId + " batch=" + batchId);

            // Recalculate totals and sync
            syncPaymentWithFeesAndStudent(studentId, batchId, amount);
            triggerFeePaidNotification(studentId, batchId, amount, "CASH");
            System.out.println("[PaymentDAO] ✅ Admin approval complete for " + studentId);
            return true;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error marking as paid: " + e.getMessage());
            return false;
        }
    }

    private void syncPaymentWithFeesAndStudent(String studentId, int batchId, double amount) {
        com.mongodb.client.MongoDatabase database = db.DBConnection.getDatabase();
        if (database == null) return;

        // STEP 2: Update Fees Collection Status
        database.getCollection("fees").updateOne(
            Filters.and(Filters.eq("student_id", studentId), Filters.eq("batch_id", batchId)),
            new Document("$set", new Document("status", "PAID")),
            new com.mongodb.client.model.UpdateOptions().upsert(true)
        );

        // STEP 3: Recompute Student Totals (Single Source of Truth)
        recalculateStudentFees(studentId);
    }

    public void recalculateStudentFees(String studentId) {
        com.mongodb.client.MongoDatabase database = db.DBConnection.getDatabase();
        if (database == null) return;

        try {
            // A. Calculate Total Paid — reads standard schema: "amount" field, status=SUCCESS
            //    Also accepts legacy status=PAID for backward compat with old records
            List<Document> allPayments = database.getCollection("payments").find(
                Filters.and(
                    Filters.eq("student_id", studentId),
                    Filters.or(
                        Filters.eq("status", "SUCCESS"),
                        Filters.eq("status", "PAID")
                    )
                )
            ).into(new ArrayList<>());

            double totalPaid = 0;
            for (Document p : allPayments) {
                // Standard field: "amount". Fallback: "amount_paid" for old records.
                Object amt = p.get("amount");
                if (amt == null) amt = p.get("amount_paid");
                if (amt instanceof Number) totalPaid += ((Number) amt).doubleValue();
            }
            System.out.println("[PaymentDAO] Student " + studentId + " → Total Paid: " + totalPaid);

            // B. Calculate Total Fees from Active Enrollments
            // B. Fetch enrollments — OR across all 3 field name variants
            List<Document> enrollments = database.getCollection("enrollments").find(
                Filters.and(
                    Filters.or(
                        Filters.eq("student_user_id", studentId),
                        Filters.eq("student_id", studentId),
                        Filters.eq("user_id", studentId)
                    ),
                    Filters.regex("status", "^ACTIVE$", "i")
                )
            ).into(new ArrayList<>());

            double totalFees = 0;
            for (Document e : enrollments) {
                Integer bId = e.getInteger("batch_id");
                Document batch = database.getCollection("batches").find(
                    Filters.or(
                        Filters.eq("_id", bId),
                        Filters.eq("_id", String.valueOf(bId))
                    )
                ).first();
                if (batch != null) {
                    Integer subjectId = batch.getInteger("subject_id");
                    Document subject = database.getCollection("subjects").find(
                        Filters.or(
                            Filters.eq("_id", subjectId),
                            Filters.eq("_id", String.valueOf(subjectId))
                        )
                    ).first();
                    if (subject != null) {
                        Object fee = subject.get("monthly_fee");
                        if (fee instanceof Number) totalFees += ((Number) fee).doubleValue();
                    }
                }
            }

            double pending = totalFees - totalPaid;
            if (pending < 0) pending = 0;

            // C. Update Students Collection
            database.getCollection("students").updateOne(
                Filters.eq("_id", studentId),
                new Document("$set", new Document("paid_fees", totalPaid)
                    .append("total_fees", totalFees)
                    .append("pending_fees", pending))
            );

            System.out.println("[PaymentDAO] Recomputed fees for " + studentId + ": Total=" + totalFees + ", Paid=" + totalPaid + ", Pending=" + pending);
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error recalculating fees: " + e.getMessage());
        }
    }
    public java.util.List<Payment> getAllPaymentsForStudent(String studentId) {
        java.util.List<Payment> list = new java.util.ArrayList<>();
        if (paymentCollection == null) return list;
        try {
            com.mongodb.client.MongoCursor<Document> cursor = paymentCollection.find(
                Filters.or(
                    Filters.eq("student_id", studentId),
                    Filters.eq("studentId", studentId)
                )
            ).iterator();
            while (cursor.hasNext()) {
                list.add(db.DocumentMapper.documentToPayment(cursor.next()));
            }
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Error fetching all payments: " + e.getMessage());
        }
        return list;
    }

    private void triggerFeePaidNotification(String studentId, int batchId, double amount, String mode) {
        new Thread(() -> {
            try {
                com.mongodb.client.MongoDatabase database = db.DBConnection.getDatabase();
                if (database == null) return;
                
                Document studentDoc = database.getCollection("students").find(
                    Filters.eq("_id", studentId)
                ).first();
                
                if (studentDoc != null) {
                    String parentId = studentDoc.getString("parent_user_id");
                    if (parentId == null) {
                        Document parentEmbed = (Document) studentDoc.get("parent");
                        parentId = parentEmbed != null ? parentEmbed.getString("parent_id") : null;
                    }
                    String studentName = studentDoc.getString("full_name");
                    
                    String batchName = "Batch " + batchId;
                    Document batch = database.getCollection("batches").find(
                        Filters.eq("_id", batchId)
                    ).first();
                    if (batch != null) batchName = batch.getString("batch_name");
                    
                    if (parentId != null) {
                        new service.NotificationService().notifyFeePaid(
                            parentId, studentId, studentName, amount, batchName, mode
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("[PaymentDAO] Error triggering notification: " + e.getMessage());
            }
        }).start();
    }
}


package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import db.DocumentMapper;
import model.Student;

/**
 * Fee Analytics DAO - Handles aggregation for fee-related analytics
 */
public class FeeAnalyticsDAO {

    private MongoCollection<Document> studentCollection;
    private MongoCollection<Document> paymentCollection;
    private MongoCollection<Document> enrollmentCollection;

    public FeeAnalyticsDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            this.studentCollection = database.getCollection("students");
            this.paymentCollection = database.getCollection("payments");
            this.enrollmentCollection = database.getCollection("enrollments");
        }
    }

    /**
     * Get all students with their consolidated fee status.
     * Logic: 
     * - Fetch all students.
     * - For each student, check their enrollments.
     * - Check if payments exist for those enrollments.
     * - If enrollments exist and all are paid -> PAID.
     * - If enrollments exist and some are unpaid -> UNPAID.
     * - If no enrollments -> NO_ENROLLMENT.
     */
    public List<Map<String, Object>> getAllStudentFeeStatus(String filterMonth, String filterYear) {
        List<Map<String, Object>> report = new ArrayList<>();
        if (enrollmentCollection == null) return report;

        // Parse year and month numbers
        int targetYear;
        int targetMonth;
        try {
            if (filterMonth.contains("-")) {
                String[] parts = filterMonth.split("-");
                targetYear = Integer.parseInt(parts[0]);
                targetMonth = Integer.parseInt(parts[1]);
            } else {
                targetYear = Integer.parseInt(filterYear);
                targetMonth = Integer.parseInt(filterMonth);
            }
        } catch (Exception e) {
            // Fallback to current if parsing fails
            java.util.Calendar cal = java.util.Calendar.getInstance();
            targetYear = cal.get(java.util.Calendar.YEAR);
            targetMonth = cal.get(java.util.Calendar.MONTH) + 1;
        }

        List<Document> pipeline = new ArrayList<>();
        
        // 1. Start from Active Enrollments
        pipeline.add(new Document("$match", new Document("status", "ACTIVE")));

        // 2. Join with Students
        pipeline.add(new Document("$lookup", new Document("from", "students")
            .append("localField", "student_id")
            .append("foreignField", "_id")
            .append("as", "student")));
        pipeline.add(new Document("$unwind", new Document("path", "$student").append("preserveNullAndEmptyArrays", true)));

        // 3. Join with Batches
        pipeline.add(new Document("$lookup", new Document("from", "batches")
            .append("localField", "batch_id")
            .append("foreignField", "_id")
            .append("as", "batch")));
        pipeline.add(new Document("$unwind", new Document("path", "$batch").append("preserveNullAndEmptyArrays", true)));

        // 4. Join with Subjects
        pipeline.add(new Document("$lookup", new Document("from", "subjects")
            .append("localField", "batch.subject_id")
            .append("foreignField", "_id")
            .append("as", "subject")));
        pipeline.add(new Document("$unwind", new Document("path", "$subject").append("preserveNullAndEmptyArrays", true)));

        // 5. Join with Payments for specific month
        // We match on enrollment_id. Handle both exact match and formatted string match (E001)
        Document paymentLookup = new Document("from", "payments");
        paymentLookup.append("let", new Document("eid", "$_id")); // Enrollment ID
        paymentLookup.append("pipeline", java.util.Arrays.asList(
            new Document("$match", new Document("$expr", new Document("$and", java.util.Arrays.asList(
                new Document("$or", java.util.Arrays.asList(
                    new Document("$eq", java.util.Arrays.asList("$enrollment_id", "$$eid")),
                    new Document("$eq", java.util.Arrays.asList(
                        new Document("$convert", new Document("input", new Document("$substrCP", java.util.Arrays.asList("$enrollment_id", 1, 3)))
                            .append("to", "int")
                            .append("onError", -1)
                            .append("onNull", -1)),
                        "$$eid"
                    ))
                )),
                new Document("$eq", java.util.Arrays.asList(new Document("$year", "$payment_date"), targetYear)),
                new Document("$eq", java.util.Arrays.asList(new Document("$month", "$payment_date"), targetMonth))
            ))))
        ));
        paymentLookup.append("as", "payments_found");
        pipeline.add(new Document("$lookup", paymentLookup));

        for (Document doc : enrollmentCollection.aggregate(pipeline)) {
            Map<String, Object> data = new HashMap<>();
            
            Document student = (Document) doc.get("student");
            Document batch = (Document) doc.get("batch");
            Document subject = (Document) doc.get("subject");
            List<Document> payments = doc.getList("payments_found", Document.class);
            
            String studentName = student != null ? (student.getString("full_name") != null ? student.getString("full_name") : student.getString("name")) : "Unknown";
            String studentId = student != null ? student.getString("_id") : String.valueOf(doc.get("student_id"));
            String batchName = batch != null ? batch.getString("batch_name") : "Batch " + doc.get("batch_id");
            
            // Calculate total paid this month
            double amountPaid = 0.0;
            String paymentMode = "—";
            String paymentDate = "—";
            String dbStatus = "UNPAID";

            if (payments != null && !payments.isEmpty()) {
                for (Document p : payments) {
                    // Capture DB status if it's PAID or SUCCESS
                    String s = p.getString("status");
                    if ("PAID".equalsIgnoreCase(s) || "SUCCESS".equalsIgnoreCase(s)) {
                        dbStatus = "PAID";
                    }

                    Object amt = p.get("amount");
                    if (amt == null) amt = p.get("amount_paid");
                    if (amt instanceof Number) amountPaid += ((Number) amt).doubleValue();
                    
                    if (paymentMode.equals("—")) {
                        paymentMode = p.getString("mode");
                        if (paymentMode == null) paymentMode = p.getString("method");
                        if (paymentMode == null) paymentMode = p.getString("payment_mode");
                    }
                    
                    if (paymentDate.equals("—")) {
                        Object d = p.get("payment_date");
                        if (d == null) d = p.get("date");
                        if (d instanceof java.util.Date) {
                            paymentDate = new java.text.SimpleDateFormat("dd-MM-yyyy").format((java.util.Date) d);
                        } else if (d != null) {
                            paymentDate = d.toString();
                        }
                    }
                }
            }
            
            // Derive Total Fee from Subject
            double totalFee = 0.0;
            if (subject != null) {
                // Try flat field first
                Object fee = subject.get("monthly_fee");
                if (fee instanceof Number) {
                    totalFee = ((Number) fee).doubleValue();
                } else {
                    // Try nested fees object
                    Document feesObj = (Document) subject.get("fees");
                    if (feesObj != null) {
                        String standard = doc.getString("class_standard");
                        if (standard == null) standard = batch != null ? batch.getString("standard") : "";
                        
                        Object classFee = feesObj.get("class_" + standard);
                        if (classFee instanceof Number) {
                            totalFee = ((Number) classFee).doubleValue();
                        }
                    }
                }
            }
            
            // Determine Status
            String finalStatus = "UNPAID";
            if ("PAID".equals(dbStatus) || (amountPaid >= totalFee && totalFee > 0)) {
                finalStatus = "PAID";
            } else if (amountPaid > 0) {
                finalStatus = "PARTIAL";
            } else {
                finalStatus = "UNPAID";
            }

            data.put("student_name", studentName);
            data.put("student_id", studentId);
            data.put("batch_name", batchName);
            data.put("payment_amount", amountPaid);
            data.put("payment_status", finalStatus);
            data.put("payment_mode", paymentMode != null ? paymentMode : "—");
            data.put("payment_date", paymentDate);
            data.put("pending_amount", Math.max(0, totalFee - amountPaid));
            data.put("total_fee", totalFee);
            
            report.add(data);
        }
        return report;
    }

    public Map<String, Double> getFeeSummaryStats(String filterMonth, String filterYear) {
        Map<String, Double> stats = new HashMap<>();
        List<Map<String, Object>> report = getAllStudentFeeStatus(filterMonth, filterYear);
        
        double totalCollected = 0;
        double pendingAmount = 0;
        double partialPayments = 0;
        double paidStudents = 0;
        double unpaidStudents = 0;
        double totalExpected = 0;
        
        for (Map<String, Object> r : report) {
            String status = (String) r.get("payment_status");
            double amount = (Double) r.get("payment_amount");
            double pending = (Double) r.get("pending_amount");
            double fee = (Double) r.get("total_fee");
            
            totalCollected += amount;
            pendingAmount += pending;
            totalExpected += fee;
            
            if ("PAID".equals(status)) paidStudents++;
            else if ("PARTIAL".equals(status)) partialPayments++;
            else unpaidStudents++;
        }
        
        double collectionPercentage = totalExpected > 0 ? (totalCollected / totalExpected) * 100 : 0;
        
        stats.put("totalCollected", totalCollected);
        stats.put("pendingAmount", pendingAmount);
        stats.put("partialPayments", partialPayments);
        stats.put("paidStudents", paidStudents);
        stats.put("unpaidStudents", unpaidStudents);
        stats.put("collectionPercentage", collectionPercentage);
        
        return stats;
    }
}

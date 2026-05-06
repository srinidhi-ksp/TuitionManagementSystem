package util;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import service.NotificationService;
import db.DBConnection;

public class NotificationSeeder {

    public static void seedNotificationsFromExistingPayments() {
        MongoDatabase db = DBConnection.getDatabase();
        NotificationService ns = new NotificationService();
        
        System.out.println("Starting notification seeding...");
        
        com.mongodb.client.FindIterable<Document> paidPayments = 
            db.getCollection("payments").find(
                com.mongodb.client.model.Filters.or(
                    com.mongodb.client.model.Filters.eq("status", "PAID"),
                    com.mongodb.client.model.Filters.eq("status", "SUCCESS")
                )
            );
        
        for (Document payment : paidPayments) {
            try {
                // If it's an old legacy structure it might have enrollment_id,
                // but new standard schema has student_id and batch_id directly on payment.
                String studentId = payment.getString("student_id");
                if (studentId == null) studentId = payment.getString("studentId");
                
                Object bObj = payment.get("batch_id");
                Integer batchId = null;
                if (bObj instanceof Number) batchId = ((Number) bObj).intValue();
                
                // Fallback to enrollment_id logic for old records
                if (studentId == null || batchId == null) {
                    Integer enrollmentId = payment.getInteger("enrollment_id");
                    if (enrollmentId != null) {
                        Document enrollment = db.getCollection("enrollments").find(
                            new Document("_id", enrollmentId)
                        ).first();
                        if (enrollment != null) {
                            studentId = enrollment.getString("student_id");
                            batchId = enrollment.getInteger("batch_id");
                        }
                    }
                }
                
                if (studentId == null || batchId == null) {
                    System.out.println("Skipping payment (missing student/batch): " + payment.getObjectId("_id"));
                    continue;
                }
                
                double amount = 0.0;
                Object amt = payment.get("amount");
                if (amt == null) amt = payment.get("amount_paid");
                if (amt instanceof Number) amount = ((Number) amt).doubleValue();
                
                String mode = payment.getString("method") != null ? 
                              payment.getString("method") : 
                              (payment.getString("payment_mode") != null ? payment.getString("payment_mode") : "Unknown");
                
                Document studentDoc = db.getCollection("students").find(
                    new Document("_id", studentId)
                ).first();
                if (studentDoc == null) {
                    System.out.println("No student found for: " + studentId);
                    continue;
                }
                
                String parentId = studentDoc.getString("parent_user_id");
                if (parentId == null) {
                    Document parentEmbed = (Document) studentDoc.get("parent");
                    if (parentEmbed == null) {
                        System.out.println("No parent info for student: " + studentId);
                        continue;
                    }
                    parentId = parentEmbed.getString("parent_id");
                }
                String studentName = studentDoc.getString("full_name");
                
                Document batch = db.getCollection("batches").find(
                    new Document("_id", batchId)
                ).first();
                String batchName = batch != null ? 
                                   batch.getString("batch_name") : "Batch " + batchId;
                
                ns.notifyFeePaid(parentId, studentId, studentName, 
                                 amount, batchName, mode);
                
            } catch (Exception e) {
                System.err.println("Error seeding payment: " + e.getMessage());
            }
        }
        System.out.println("Seeding complete.");
    }
    
    public static void main(String[] args) {
        seedNotificationsFromExistingPayments();
    }
}

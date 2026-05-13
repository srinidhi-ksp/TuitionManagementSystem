import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MigrateDB2 {
    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("tuitionManagementSystem");

            // Migrate Enrollments
            MongoCollection<Document> enrollments = db.getCollection("enrollments");
            List<Document> allEnrollments = enrollments.find().into(new ArrayList<>());
            for (Document doc : allEnrollments) {
                Object id = doc.get("_id");
                boolean needsUpdate = false;
                Document newDoc = new Document(doc);
                
                Object batchId = doc.get("batch_id");
                if (batchId instanceof String && ((String) batchId).startsWith("B")) {
                    int newBatchId = Integer.parseInt(((String) batchId).replaceAll("\\D+", ""));
                    newDoc.put("batch_id", newBatchId);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    enrollments.replaceOne(new Document("_id", id), newDoc);
                    System.out.println("Migrated Enrollment: " + id);
                }
            }

            // Migrate Payments
            MongoCollection<Document> payments = db.getCollection("payments");
            List<Document> allPayments = payments.find().into(new ArrayList<>());
            for (Document doc : allPayments) {
                Object id = doc.get("_id");
                boolean needsUpdate = false;
                Document newDoc = new Document(doc);
                
                Object batchId = doc.get("batch_id");
                if (batchId instanceof String && ((String) batchId).startsWith("B")) {
                    int newBatchId = Integer.parseInt(((String) batchId).replaceAll("\\D+", ""));
                    newDoc.put("batch_id", newBatchId);
                    needsUpdate = true;
                }

                Object subjectId = doc.get("subject_id");
                if (subjectId instanceof String && ((String) subjectId).startsWith("SUB")) {
                    int newSubjectId = Integer.parseInt(((String) subjectId).replaceAll("\\D+", ""));
                    newDoc.put("subject_id", newSubjectId);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    payments.replaceOne(new Document("_id", id), newDoc);
                    System.out.println("Migrated Payment: " + id);
                }
            }

            // Migrate Attendance
            MongoCollection<Document> attendance = db.getCollection("attendance");
            List<Document> allAttendance = attendance.find().into(new ArrayList<>());
            for (Document doc : allAttendance) {
                Object id = doc.get("_id");
                boolean needsUpdate = false;
                Document newDoc = new Document(doc);
                
                Object batchId = doc.get("batch_id");
                if (batchId instanceof String && ((String) batchId).startsWith("B")) {
                    int newBatchId = Integer.parseInt(((String) batchId).replaceAll("\\D+", ""));
                    newDoc.put("batch_id", newBatchId);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    attendance.replaceOne(new Document("_id", id), newDoc);
                    System.out.println("Migrated Attendance: " + id);
                }
            }
            
            System.out.println("Migration 2 complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

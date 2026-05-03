package scratch;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.Arrays;
import java.util.Date;

public class InsertMockData {
    public static void main(String[] args) {
        String uri = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("TuitionManagementSystem");

            // Students
            MongoCollection<Document> students = database.getCollection("students");
            students.deleteMany(new Document());
            students.insertMany(Arrays.asList(
                new Document("_id", "S001").append("full_name", "Arjun Kumar").append("parent_user_id", "U31").append("standard", "12").append("status", "ACTIVE"),
                new Document("_id", "S002").append("full_name", "Sanya Gupta").append("parent_user_id", "U32").append("standard", "10").append("status", "ACTIVE")
            ));

            // Batches
            MongoCollection<Document> batches = database.getCollection("batches");
            batches.deleteMany(new Document());
            batches.insertMany(Arrays.asList(
                new Document("_id", 201).append("batch_name", "Physics Class 12").append("subject_id", 101).append("teacher_id", "T001").append("timing", "MON 09:00 - 11:00").append("status", "ACTIVE"),
                new Document("_id", 202).append("batch_name", "Maths Class 12").append("subject_id", 102).append("teacher_id", "T002").append("timing", "MON 11:30 - 13:30").append("status", "ACTIVE"),
                new Document("_id", 205).append("batch_name", "Chemistry Class 12").append("subject_id", 103).append("teacher_id", "T003").append("timing", "WED 10:00 - 12:00").append("status", "ACTIVE")
            ));

            // Enrollments
            MongoCollection<Document> enrollments = database.getCollection("enrollments");
            enrollments.deleteMany(new Document());
            enrollments.insertMany(Arrays.asList(
                new Document("_id", 301).append("student_id", "S001").append("batch_id", 201).append("status", "Active").append("enrollment_date", new Date()),
                new Document("_id", 302).append("student_id", "S001").append("batch_id", 202).append("status", "ACTIVE").append("enrollment_date", new Date()),
                new Document("_id", 303).append("student_id", "S002").append("batch_id", 201).append("status", "ACTIVE").append("enrollment_date", new Date())
            ));

            // Payments
            MongoCollection<Document> payments = database.getCollection("payments");
            payments.deleteMany(new Document());
            payments.insertMany(Arrays.asList(
                new Document("student_id", "S001").append("batch_id", 201).append("month", "2026-05").append("amount", 1500.0).append("status", "PAID").append("payment_date", new Date()),
                new Document("student_id", "S001").append("batch_id", 202).append("month", "2026-05").append("amount", 1800.0).append("status", "PENDING")
            ));

            System.out.println("✅ Mock data injection complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package util;

import db.DBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.Date;

public class LegacyDataMigration {

    public static void main(String[] args) {
        System.out.println("Starting Tuition Management System Data Migration...");
        
        MongoDatabase db = DBConnection.getDatabase();
        if (db == null) {
            System.err.println("Failed to connect to database!");
            return;
        }

        MongoCollection<Document> studentsCollection = db.getCollection("students");
        MongoCollection<Document> enrollmentsCollection = db.getCollection("enrollments");
        
        int migratedCount = 0;

        // Find students with legacy 'enrollment' field (single object or array)
        try (MongoCursor<Document> cursor = studentsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document student = cursor.next();
                String studentId = student.getString("_id");
                if (studentId == null) {
                    studentId = student.getString("user_id");
                }
                
                if (studentId == null) continue;

                // Check for legacy 'enrollment' field
                Object enrollmentObj = student.get("enrollment");
                if (enrollmentObj != null) {
                    if (enrollmentObj instanceof Document) {
                        System.out.println("Found legacy single enrollment for student: " + studentId);
                        migrateSingleEnrollment((Document) enrollmentObj, studentId, enrollmentsCollection);
                        migratedCount++;
                    } else if (enrollmentObj instanceof java.util.List) {
                        System.out.println("Found legacy array enrollments for student: " + studentId);
                        java.util.List<?> list = (java.util.List<?>) enrollmentObj;
                        for (Object item : list) {
                            if (item instanceof Document) {
                                migrateSingleEnrollment((Document) item, studentId, enrollmentsCollection);
                            }
                        }
                        migratedCount++;
                    }
                    
                    // Remove the legacy field to clean up
                    studentsCollection.updateOne(
                        Filters.eq("_id", student.get("_id")), 
                        Updates.unset("enrollment")
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error during migration: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Migration complete. Total students migrated: " + migratedCount);
    }

    private static void migrateSingleEnrollment(Document oldEnr, String studentId, MongoCollection<Document> enrollmentsCollection) {
        // Create new enrollment document matching the new schema
        Document newEnr = new Document();
        
        // Generate a random ID if none exists
        int newId = (int) (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 1000);
        newEnr.put("_id", newId);
        newEnr.put("student_id", studentId);
        newEnr.put("student_user_id", studentId); // fallback
        newEnr.put("user_id", studentId); // fallback
        
        // Extract course/batch
        Integer batchId = null;
        if (oldEnr.get("batch_id") instanceof Number) {
            batchId = ((Number) oldEnr.get("batch_id")).intValue();
        } else if (oldEnr.getString("batchId") != null) {
            try { batchId = Integer.parseInt(oldEnr.getString("batchId")); } catch (Exception ignored) {}
        }
        
        if (batchId == null) {
            // Default to 1 if we can't figure it out, to avoid breaking constraints
            batchId = 1;
        }
        
        newEnr.put("batch_id", batchId);
        
        Date date = oldEnr.getDate("enrollment_date");
        if (date == null) date = new Date();
        newEnr.put("enrollment_date", date);
        
        String status = oldEnr.getString("status");
        if (status == null) status = "ACTIVE";
        newEnr.put("status", status);

        // Check if already exists to prevent duplicates
        long count = enrollmentsCollection.countDocuments(
            Filters.and(
                Filters.eq("student_id", studentId),
                Filters.eq("batch_id", batchId)
            )
        );

        if (count == 0) {
            enrollmentsCollection.insertOne(newEnr);
            System.out.println("   -> Successfully migrated batch_id: " + batchId);
        } else {
            System.out.println("   -> Skipping batch_id " + batchId + " (Already exists in enrollments collection)");
        }
    }
}

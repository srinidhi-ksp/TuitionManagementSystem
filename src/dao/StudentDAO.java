package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import db.DBConnection;
import db.DocumentMapper;
import model.Student;

public class StudentDAO {

    private MongoCollection<Document> studentCollection;

    public StudentDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            studentCollection = database.getCollection("students");
        }
    }

    public boolean addStudent(Student student) {
        if (studentCollection == null) return false;
        try {
            Document doc = DocumentMapper.studentToDocument(student);
            studentCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Student getStudentById(String userId) {
        if (studentCollection == null) return null;
        try {
            // Try both _id and user_id fields to find the student
            Document doc = studentCollection.find(
                Filters.or(
                    Filters.eq("_id", userId),
                    Filters.eq("user_id", userId)
                )
            ).first();
            
            return DocumentMapper.documentToStudent(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * CRITICAL: Robust mapping: user_id (U01) -> Student object (S001)
     * Uses multiple strategies to find student by user ID
     */
    public Student getStudentByUserId(String userIdValue) {
        if (studentCollection == null || userIdValue == null) {
            System.err.println("[StudentDAO] ❌ getStudentByUserId: Collection or ID is null");
            return null;
        }
        
        try {
            String searchId = userIdValue.trim();
            System.out.println("[StudentDAO] 🔍 Attempting to map User ID: '" + searchId + "'");
            
            // STRATEGY 1: Try exact match on 'user_id' field (if documents have this field)
            Document doc = studentCollection.find(Filters.eq("user_id", searchId)).first();
            if (doc != null) {
                System.out.println("[StudentDAO]   ✅ Found via user_id field");
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) {
                    System.out.println("[StudentDAO] ✅ Mapped " + searchId + " → Student " + s.getUserId());
                }
                return s;
            }
            System.out.println("[StudentDAO]   ⚠️  No match on user_id field");
            
            // STRATEGY 2: Try match on email (most reliable cross-reference)
            System.out.println("[StudentDAO]   🔄 Trying email-based lookup...");
            UserDAO userDAO = new UserDAO();
            model.User user = userDAO.getUserById(searchId);
            if (user != null && user.getEmail() != null) {
                String email = user.getEmail();
                System.out.println("[StudentDAO]   Searching by email: " + email);
                doc = studentCollection.find(Filters.eq("email", email)).first();
                if (doc != null) {
                    System.out.println("[StudentDAO]   ✅ Found via email field");
                    Student s = DocumentMapper.documentToStudent(doc);
                    if (s != null) {
                        System.out.println("[StudentDAO] ✅ Mapped " + searchId + " → Student " + s.getUserId());
                    }
                    return s;
                }
            }
            System.out.println("[StudentDAO]   ⚠️  No match on email field");

            // STRATEGY 3: Fallback - Check if the provided ID is already the Student ID (_id)
            if (searchId.startsWith("S")) {
                System.out.println("[StudentDAO]   🔄 ID starts with 'S', trying as student_id...");
                doc = studentCollection.find(Filters.eq("_id", searchId)).first();
                if (doc != null) {
                    System.out.println("[StudentDAO]   ✅ Found via _id field (already student_id)");
                    Student s = DocumentMapper.documentToStudent(doc);
                    if (s != null) {
                        System.out.println("[StudentDAO] ✅ Mapped " + searchId + " → Student " + s.getUserId());
                    }
                    return s;
                }
            }

            System.err.println("[StudentDAO] ❌ FAILED to map User ID: " + searchId + " (all strategies exhausted)");
            return null;
            
        } catch (Exception e) {
            System.err.println("[StudentDAO] ❌ Error in getStudentByUserId: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Student getStudentByEmail(String email) {
        if (studentCollection == null) return null;
        try {
            Document doc = studentCollection.find(
                Filters.eq("email", email)
            ).first();
            return DocumentMapper.documentToStudent(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean deleteStudent(String userId) {
        if (studentCollection == null) return false;
        try {
            long deletedCount = studentCollection.deleteOne(
                Filters.or(
                    Filters.eq("_id", userId),
                    Filters.eq("user_id", userId)
                )
            ).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStudent(Student student) {
        if (studentCollection == null) return false;
        try {
            Document doc = DocumentMapper.studentToDocument(student);
            long matched = studentCollection.replaceOne(
                Filters.or(
                    Filters.eq("_id",     student.getUserId()),
                    Filters.eq("user_id", student.getUserId())
                ),
                doc,
                new ReplaceOptions().upsert(false)
            ).getMatchedCount();
            return matched > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Student> getAllStudents() {
        List<Student> studentList = new ArrayList<>();
        if (studentCollection == null) return studentList;
        
        try (MongoCursor<Document> cursor = studentCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) {
                    studentList.add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return studentList;
    }

    public List<Student> getStudentsByIds(List<String> userIds) {
        List<Student> studentList = new ArrayList<>();
        if (studentCollection == null || userIds.isEmpty()) return studentList;

        try (MongoCursor<Document> cursor = studentCollection.find(Filters.in("user_id", userIds)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Student s = DocumentMapper.documentToStudent(doc);
                if (s != null) studentList.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return studentList;
    }
}

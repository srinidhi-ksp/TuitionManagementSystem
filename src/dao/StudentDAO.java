package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import model.Student;
import db.DBConnection;
import db.DocumentMapper;

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

    // Robust mapping: user_id (U01) -> Student object (S001)
    public Student getStudentByUserId(String userIdValue) {
        if (studentCollection == null || userIdValue == null) return null;
        try {
            System.out.println("[StudentDAO] Attempting to map User ID: " + userIdValue);
            
            // 1. Try exact match on 'user_id' field
            Document doc = studentCollection.find(Filters.eq("user_id", userIdValue)).first();
            
            // 2. Try match on email (most reliable)
            if (doc == null) {
                UserDAO userDAO = new UserDAO();
                model.User user = userDAO.getUserById(userIdValue);
                if (user != null && user.getEmail() != null) {
                    System.out.println("[StudentDAO] Searching by email: " + user.getEmail());
                    doc = studentCollection.find(Filters.eq("email", user.getEmail())).first();
                }
            }

            // 3. Fallback: Check if the provided ID is already the Student ID (_id)
            if (doc == null && userIdValue.startsWith("S")) {
                doc = studentCollection.find(Filters.eq("_id", userIdValue)).first();
            }

            Student s = DocumentMapper.documentToStudent(doc);
            if (s != null) {
                System.out.println("[StudentDAO] ✅ Mapped " + userIdValue + " to Student " + s.getUserId());
            } else {
                System.out.println("[StudentDAO] ❌ Failed to map User ID: " + userIdValue);
            }
            return s;
        } catch (Exception e) {
            System.err.println("[StudentDAO] Error mapping user to student: " + e.getMessage());
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

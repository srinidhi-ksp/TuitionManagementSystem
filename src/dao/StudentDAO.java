package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

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

    // Look up student strictly by the 'user_id' field (e.g., "U01" from enrollments)
    public Student getStudentByUserId(String userIdValue) {
        if (studentCollection == null) return null;
        try {
            Document doc = studentCollection.find(
                Filters.eq("user_id", userIdValue)
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
}

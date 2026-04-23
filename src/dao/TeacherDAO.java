package dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import db.DBConnection;
import db.DocumentMapper;
import model.Teacher;

public class TeacherDAO {

    private MongoCollection<Document> teacherCollection;
    private UserDAO userDAO; // Add UserDAO for join date lookup

    public TeacherDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            teacherCollection = database.getCollection("teachers");
        }
        this.userDAO = new UserDAO(); // Initialize UserDAO
    }

    public boolean addTeacher(Teacher teacher) {
        if (teacherCollection == null) return false;
        try {
            Document doc = DocumentMapper.teacherToDocument(teacher);
            teacherCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Teacher getTeacherById(String userId) {
        if (teacherCollection == null) return null;
        try {
            Document doc = teacherCollection.find(
                Filters.or(
                    Filters.eq("_id", userId),
                    Filters.eq("user_id", userId)
                )
            ).first();
            return DocumentMapper.documentToTeacher(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteTeacher(String userId) {
        if (teacherCollection == null) return false;
        try {
            long deletedCount = teacherCollection.deleteOne(
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

    public boolean updateTeacher(Teacher teacher) {
        if (teacherCollection == null) return false;
        try {
            Document doc = DocumentMapper.teacherToDocument(teacher);
            long matched = teacherCollection.replaceOne(
                Filters.or(
                    Filters.eq("_id",     teacher.getUserId()),
                    Filters.eq("user_id", teacher.getUserId())
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

    public List<Teacher> getAllTeachers() {
        List<Teacher> teacherList = new ArrayList<>();
        if (teacherCollection == null) return teacherList;
        
        try (MongoCursor<Document> cursor = teacherCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Teacher t = DocumentMapper.documentToTeacher(doc);
                if (t != null) {
                    // ✅ Fetch join date from users collection using user_id OR email
                    Date joinDate = null;
                    
                    // 1. Try using 'user_id' field (as per database schema)
                    String userRefId = doc.getString("user_id");
                    if (userRefId != null && !userRefId.isEmpty()) {
                        joinDate = userDAO.getCreatedAt(userRefId);
                        System.out.println("[TeacherDAO] Fetching join date via user_id: " + userRefId + " -> " + joinDate);
                    }
                    
                    // 2. Fallback to email lookup if user_id didn't work
                    if (joinDate == null && t.getEmail() != null) {
                        joinDate = userDAO.getCreatedAtByEmail(t.getEmail());
                        System.out.println("[TeacherDAO] Fallback: join date via email: " + t.getEmail() + " -> " + joinDate);
                    }
                    
                    // 3. Last fallback: try the teacher's own ID
                    if (joinDate == null) {
                        joinDate = userDAO.getCreatedAt(t.getUserId());
                    }
                    
                    t.setJoinDate(joinDate);
                    teacherList.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teacherList;
    }
}

package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import model.Teacher;
import db.DBConnection;
import db.DocumentMapper;

public class TeacherDAO {

    private MongoCollection<Document> teacherCollection;

    public TeacherDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            teacherCollection = database.getCollection("teachers");
        }
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
                    teacherList.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teacherList;
    }
}

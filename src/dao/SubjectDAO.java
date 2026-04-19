package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.Subject;
import db.DBConnection;
import db.DocumentMapper;

public class SubjectDAO {

    private MongoCollection<Document> subjectCollection;

    public SubjectDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            subjectCollection = database.getCollection("subjects");
        }
    }

    public boolean addSubject(Subject subject) {
        if (subjectCollection == null) return false;
        try {
            Document doc = DocumentMapper.subjectToDocument(subject);
            subjectCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Subject getSubjectById(int subjectId) {
        if (subjectCollection == null) return null;
        try {
            Document doc = subjectCollection.find(Filters.eq("_id", subjectId)).first();
            return DocumentMapper.documentToSubject(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        if (subjectCollection == null) return list;

        try (MongoCursor<Document> cursor = subjectCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Subject s = DocumentMapper.documentToSubject(doc);
                if (s != null) {
                    list.add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteSubject(int subjectId) {
        if (subjectCollection == null) return false;
        try {
            long deletedCount = subjectCollection.deleteOne(Filters.eq("_id", subjectId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.TestMark;
import db.DBConnection;
import db.DocumentMapper;

public class MarksDAO {
    private MongoCollection<Document> marksCollection;

    public MarksDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            marksCollection = database.getCollection("marks");
        }
    }

    public List<TestMark> getMarksForStudent(String userId) {
        List<TestMark> list = new ArrayList<>();
        if (marksCollection == null) return list;

        TestsDAO testsDao = new TestsDAO();
        BatchDAO batchDao = new BatchDAO();

        try (MongoCursor<Document> cursor = marksCollection.find(Filters.eq("user_id", userId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                model.Mark m = DocumentMapper.documentToMark(doc);
                if (m != null) {
                    TestMark tm = new TestMark();
                    tm.setMarkId(m.getMarkId());
                    tm.setTestId(m.getTestId());
                    tm.setMarksObtained(m.getMarksObtained());
                    tm.setRemarks(m.getRemarks());

                    // Manual join resolution
                    model.Test t = null;
                    MongoDatabase database = DBConnection.getDatabase();
                    if (database != null) {
                        MongoCollection<Document> testsCollection = database.getCollection("tests");
                        Document testDoc = testsCollection.find(Filters.eq("_id", m.getTestId())).first();
                        t = DocumentMapper.documentToTest(testDoc);
                    }

                    if (t != null) {
                        tm.setTestName(t.getTestName());
                        tm.setTestDate(t.getTestDate());
                        tm.setMaxMarks(t.getMaxMarks());

                        model.Batch b = batchDao.getBatchById(t.getBatchId());
                        if (b != null) {
                            // subject name needs SubjectDAO
                            SubjectDAO subjectDao = new SubjectDAO();
                            model.Subject s = subjectDao.getSubjectById(b.getSubjectId());
                            if (s != null) {
                                tm.setSubjectName(s.getSubjectName());
                            }
                        }
                    }
                    list.add(tm);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Sort the list by testDate descending
        list.sort((a, b) -> {
            if (a.getTestDate() == null || b.getTestDate() == null) return 0;
            return b.getTestDate().compareTo(a.getTestDate());
        });

        return list;
    }
}

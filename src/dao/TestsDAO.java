package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.Test;
import model.Mark;
import db.DBConnection;
import db.DocumentMapper;

public class TestsDAO {

    private MongoCollection<Document> testsCollection;
    private MongoCollection<Document> marksCollection;

    public TestsDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            testsCollection = database.getCollection("tests");
            marksCollection = database.getCollection("marks");
        }
    }

    public boolean addTest(Test t) {
        if (testsCollection == null) return false;
        try {
            Document doc = DocumentMapper.testToDocument(t);
            testsCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Test> getTestsByBatchId(int batchId) {
        List<Test> tests = new ArrayList<>();
        if (testsCollection == null) return tests;

        try (MongoCursor<Document> cursor = testsCollection.find(Filters.eq("batch_id", batchId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Test t = DocumentMapper.documentToTest(doc);
                if (t != null) {
                    tests.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tests;
    }

    public boolean saveMark(int testId, String userId, int marksObtained) {
        if (marksCollection == null) return false;
        try {
            Document existing = marksCollection.find(Filters.and(
                    Filters.eq("test_id", testId),
                    Filters.eq("user_id", userId)
            )).first();

            if (existing != null) {
                Document update = new Document("$set", new Document("marks_obtained", marksObtained));
                marksCollection.updateOne(Filters.eq("_id", existing.get("_id")), update);
                return true;
            } else {
                Mark m = new Mark();
                m.setMarkId((int)(Math.random() * 100000));
                m.setTestId(testId);
                m.setUserId(userId);
                m.setMarksObtained(marksObtained);
                Document doc = DocumentMapper.markToDocument(m);
                marksCollection.insertOne(doc);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Test> getTestsByStudentId(String studentId) {
        List<Test> tests = new ArrayList<>();
        if (testsCollection == null) return tests;

        try (MongoCursor<Document> cursor = testsCollection.find(Filters.eq("attempts.student_id", studentId)).iterator()) {
            while (cursor.hasNext()) {
                Test t = DocumentMapper.documentToTest(cursor.next());
                if (t != null) tests.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tests;
    }
    public List<model.TestMark> getStudentMarks(String studentId) {
        List<model.TestMark> testMarks = new ArrayList<>();
        if (marksCollection == null) return testMarks;

        try {
            // Find all marks for the student
            com.mongodb.client.MongoCursor<Document> cursor = marksCollection.find(com.mongodb.client.model.Filters.eq("user_id", studentId)).iterator();
            while (cursor.hasNext()) {
                Document markDoc = cursor.next();
                
                // For each mark, find the test details
                Integer testId = markDoc.getInteger("test_id");
                if (testId != null) {
                    Document testDoc = testsCollection.find(com.mongodb.client.model.Filters.eq("test_id", testId)).first();
                    
                    if (testDoc != null) {
                        model.TestMark tm = new model.TestMark();
                        tm.setMarkId(markDoc.getInteger("mark_id"));
                        tm.setTestId(testId);
                        tm.setTestName(testDoc.getString("test_name"));
                        tm.setSubjectName(testDoc.getString("subject_name"));
                        tm.setMaxMarks(testDoc.getInteger("max_marks"));
                        tm.setMarksObtained(markDoc.getInteger("marks_obtained"));
                        tm.setTestDate(testDoc.getDate("test_date"));
                        testMarks.add(tm);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testMarks;
    }
}

package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import db.DBConnection;
import db.DocumentMapper;
import model.Mark;
import model.Test;

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

    public boolean addTeacherTest(Test t, String teacherId) {
        if (testsCollection == null) return false;
        try {
            Document doc = DocumentMapper.testToDocument(t);
            doc.append("teacher_id", teacherId);
            doc.append("total_marks", t.getMaxMarks());
            doc.append("date", t.getTestDate());
            if (!doc.containsKey("attempts")) doc.append("attempts", new ArrayList<Document>());
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

    public List<Test> getTestsByTeacherAndBatch(String teacherId, int batchId) {
        List<Test> tests = new ArrayList<>();
        if (testsCollection == null) return tests;
        try (MongoCursor<Document> cursor = testsCollection.find(
                Filters.and(
                    Filters.eq("teacher_id", teacherId),
                    Filters.eq("batch_id", batchId)
                )
            ).iterator()) {
            while (cursor.hasNext()) {
                Test t = DocumentMapper.documentToTest(cursor.next());
                if (t != null) tests.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tests;
    }

    public int countPendingEvaluationsByTeacher(String teacherId) {
        if (testsCollection == null) return 0;
        try {
            return (int) testsCollection.countDocuments(
                Filters.and(
                    Filters.eq("teacher_id", teacherId),
                    Filters.eq("attempts.status", "PENDING")
                )
            );
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<Test> getPendingTestsByTeacher(String teacherId) {
        List<Test> tests = new ArrayList<>();
        if (testsCollection == null) return tests;
        try (MongoCursor<Document> cursor = testsCollection.find(
                Filters.and(
                    Filters.eq("teacher_id", teacherId),
                    Filters.eq("attempts.status", "PENDING")
                )
            ).iterator()) {
            while (cursor.hasNext()) {
                Test t = DocumentMapper.documentToTest(cursor.next());
                if (t != null) tests.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tests;
    }

    public Integer getExistingScore(int testId, String studentId) {
        if (testsCollection == null) return null;
        try {
            Document doc = testsCollection.find(
                Filters.and(
                    Filters.eq("_id", testId),
                    Filters.eq("attempts.student_id", studentId)
                )
            ).first();
            if (doc == null) return null;
            List<Document> attempts = doc.getList("attempts", Document.class);
            if (attempts == null) return null;
            for (Document attempt : attempts) {
                if (studentId.equals(attempt.getString("student_id"))) {
                    Object score = attempt.get("score");
                    if (score instanceof Number) return ((Number) score).intValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveAttempt(int testId, String studentId, int score) {
        if (testsCollection == null) return false;
        try {
            long matched = testsCollection.updateOne(
                Filters.and(
                    Filters.eq("_id", testId),
                    Filters.eq("attempts.student_id", studentId)
                ),
                Updates.combine(
                    Updates.set("attempts.$.score", score),
                    Updates.set("attempts.$.status", "EVALUATED"),
                    Updates.set("attempts.$.date", new java.util.Date())
                )
            ).getMatchedCount();

            if (matched == 0) {
                Document attempt = new Document()
                    .append("student_id", studentId)
                    .append("score", score)
                    .append("status", "EVALUATED")
                    .append("date", new java.util.Date());
                testsCollection.updateOne(Filters.eq("_id", testId), Updates.push("attempts", attempt));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
    /**
     * CORRECT AGGREGATION: Get all marks for a student with full details
     * 
     * Pipeline:
     * 1. $unwind attempts to explode the array
     * 2. $match student_id AND EVALUATED status AND score NOT null
     * 3. $lookup batches to get subject_id
     * 4. $lookup subjects to get subject name
     * 5. $project final fields with percentage calculation
     * 6. $sort by test_date descending
     */
    public List<model.TestMark> getStudentMarks(String studentId) {
        List<model.TestMark> testMarks = new ArrayList<>();
        if (testsCollection == null) {
            System.err.println("[TestsDAO] Tests collection is null!");
            return testMarks;
        }

        if (studentId == null || studentId.trim().isEmpty()) {
            System.err.println("[TestsDAO] Student ID is null or empty!");
            return testMarks;
        }

        try {
            System.out.println("[TestsDAO] ⏳ Fetching marks for student: " + studentId);

            List<org.bson.conversions.Bson> pipeline = java.util.Arrays.asList(
                // STEP 1: Unwind attempts array to get individual attempts
                new Document("$unwind", "$attempts"),
                
                // STEP 2: Match student_id, EVALUATED status, and non-null scores
                new Document("$match", new Document()
                    .append("attempts.student_id", studentId)
                    .append("attempts.status", "EVALUATED")
                    .append("attempts.score", new Document("$ne", null))),
                
                // STEP 3: Join with batches to get subject_id
                new Document("$lookup", new Document()
                    .append("from", "batches")
                    .append("localField", "batch_id")
                    .append("foreignField", "_id")
                    .append("as", "batch")),
                new Document("$unwind", new Document()
                    .append("path", "$batch")
                    .append("preserveNullAndEmptyArrays", false)),
                
                // STEP 4: Join with subjects to get subject name
                new Document("$lookup", new Document()
                    .append("from", "subjects")
                    .append("localField", "batch.subject_id")
                    .append("foreignField", "_id")
                    .append("as", "subject")),
                new Document("$unwind", new Document()
                    .append("path", "$subject")
                    .append("preserveNullAndEmptyArrays", false)),
                
                // STEP 5: Project final fields with calculations
                new Document("$project", new Document()
                    .append("test_name", 1)
                    .append("test_date", 1)
                    .append("total_marks", new Document("$cond", 
                        new Document("if", new Document("$ne", java.util.Arrays.asList(
                            new Document("$type", "$total_marks"), "null")))
                            .append("then", "$total_marks")
                            .append("else", 100)))
                    .append("marks_obtained", "$attempts.score")
                    .append("subject_name", "$subject.name")
                    .append("percentage", new Document("$multiply", java.util.Arrays.asList(
                        new Document("$divide", java.util.Arrays.asList("$attempts.score", 100)),
                        100)))),
                
                // STEP 6: Sort by test_date descending
                new Document("$sort", new Document("test_date", -1))
            );

            MongoCursor<Document> cursor = testsCollection.aggregate(pipeline).iterator();
            int count = 0;
            
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                count++;
                
                model.TestMark tm = new model.TestMark();
                
                // Extract fields
                tm.setTestName(doc.getString("test_name"));
                tm.setTestDate(doc.getDate("test_date"));
                
                // Handle total_marks: use default 100 if null
                int max = doc.getInteger("total_marks", 100);
                Integer obtained = doc.getInteger("marks_obtained");
                
                // ⚠️ SKIP if obtained is null (prevent NullPointerException)
                if (obtained == null) {
                    System.out.println("[TestsDAO]   ⚠️  Skipping: Null score for test: " + tm.getTestName());
                    continue;
                }
                
                tm.setMaxMarks(max);
                tm.setMarksObtained(obtained);
                
                // Subject name with fallback
                String subject = doc.getString("subject_name");
                tm.setSubjectName(subject != null ? subject : "General");
                
                // Calculate percentage and grade
                double percentage = max > 0 ? (obtained * 100.0 / max) : 0;
                tm.setPercentage(percentage);
                tm.setGrade(calculateGrade(percentage));
                
                testMarks.add(tm);
                System.out.println("[TestsDAO]   ✔ Added: " + tm.getTestName() + " - " + obtained + "/" + max);
            }
            
            System.out.println("[TestsDAO] ✅ Successfully fetched " + count + " marks for student: " + studentId);
            
        } catch (Exception e) {
            System.err.println("[TestsDAO] ❌ Aggregation Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return testMarks;
    }

    /**
     * Calculate grade based on percentage
     * A+: 90+, A: 80+, B: 70+, C: 60+, D: < 60
     */
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        return "D";
    }
}

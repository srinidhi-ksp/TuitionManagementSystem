package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.ChapterProgress;
import db.DBConnection;
import db.DocumentMapper;

public class SyllabusProgressDAO {
    private MongoCollection<Document> progressCollection;
    private MongoCollection<Document> enrollmentsCollection;
    
    public SyllabusProgressDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            progressCollection = database.getCollection("chapter_progress");
            enrollmentsCollection = database.getCollection("enrollments");
        }
    }

    public List<ChapterProgress> getProgressForStudent(String userId) {
        List<ChapterProgress> list = new ArrayList<>();
        if (progressCollection == null || enrollmentsCollection == null) return list;

        try {
            // Find batch IDs for student
            List<Integer> batchIds = new ArrayList<>();
            try (MongoCursor<Document> cursor = enrollmentsCollection.find(Filters.eq("student_user_id", userId)).iterator()) {
                while(cursor.hasNext()) {
                    Document doc = cursor.next();
                    Object bObj = doc.get("batch_id");
                    if (bObj instanceof Number) {
                        batchIds.add(((Number) bObj).intValue());
                    } else if (bObj != null) {
                        try {
                            batchIds.add(Integer.parseInt(bObj.toString()));
                        } catch(Exception ignored) {}
                    }
                }
            }

            if (batchIds.isEmpty()) return list;

            BatchDAO batchDao = new BatchDAO();
            SubjectDAO subjectDao = new SubjectDAO();
            
            for (int batchId : batchIds) {
                model.Batch b = batchDao.getBatchById(batchId);
                String subName = "";
                if (b != null) {
                    model.Subject s = subjectDao.getSubjectById(b.getSubjectId());
                    if (s != null) {
                        subName = s.getSubjectName();
                    }
                }
                
                try (MongoCursor<Document> cursor = progressCollection.find(Filters.eq("batch_id", batchId)).iterator()) {
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        ChapterProgress cp = DocumentMapper.documentToChapterProgress(doc);
                        if (cp != null) {
                            cp.setSubjectName(subName);
                            list.add(cp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<ChapterProgress> getProgressByBatchId(int batchId) {
        List<ChapterProgress> list = new ArrayList<>();
        if (progressCollection == null) return list;

        BatchDAO batchDao = new BatchDAO();
        SubjectDAO subjectDao = new SubjectDAO();
        model.Batch b = batchDao.getBatchById(batchId);
        String subName = "";
        if (b != null) {
            model.Subject s = subjectDao.getSubjectById(b.getSubjectId());
            if (s != null) {
                subName = s.getSubjectName();
            }
        }

        try (MongoCursor<Document> cursor = progressCollection.find(Filters.eq("batch_id", batchId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                ChapterProgress cp = DocumentMapper.documentToChapterProgress(doc);
                if (cp != null) {
                    cp.setSubjectName(subName);
                    list.add(cp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateProgress(int progressId, int percentage, String status) {
        if (progressCollection == null) return false;
        try {
            Document update = new Document("$set", new Document("completion_percentage", percentage)
                                                .append("status", status)
                                                .append("last_updated", new java.util.Date()));
            long modifiedCount = progressCollection.updateOne(Filters.eq("_id", progressId), update).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

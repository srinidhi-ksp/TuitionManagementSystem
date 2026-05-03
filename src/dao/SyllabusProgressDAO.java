package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import model.ChapterProgress;
import db.DBConnection;
import db.DocumentMapper;

public class SyllabusProgressDAO {
    private MongoCollection<Document> progressCollection;
    private MongoCollection<Document> enrollmentsCollection;
    private MongoCollection<Document> batchCollection;
    
    public SyllabusProgressDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            progressCollection = database.getCollection("chapter_progress");
            enrollmentsCollection = database.getCollection("enrollments");
            batchCollection = database.getCollection("batches");
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

    public List<ChapterProgress> getProgressByTeacherBatch(int batchId, String teacherId) {
        List<ChapterProgress> list = new ArrayList<>();
        if (batchCollection == null || teacherId == null) return list;
        try {
            Document batchDoc = batchCollection.find(
                Filters.and(Filters.eq("_id", batchId), Filters.eq("teacher_id", teacherId))
            ).first();
            if (batchDoc == null) return list;

            model.Batch batch = DocumentMapper.documentToBatch(batchDoc);
            if (batch == null) return list;

            SubjectDAO subjectDAO = new SubjectDAO();
            model.Subject subject = subjectDAO.getSubjectById(batch.getSubjectId());
            if (subject == null || subject.getChapters() == null) return list;

            java.util.Map<Integer, Document> progressByChapter = new java.util.HashMap<>();
            List<Document> progressDocs = batchDoc.getList("syllabus_progress", Document.class);
            if (progressDocs != null) {
                for (Document progress : progressDocs) {
                    Object chapterObj = progress.get("chapter_id");
                    if (chapterObj instanceof Number) {
                        progressByChapter.put(((Number) chapterObj).intValue(), progress);
                    }
                }
            }

            for (model.Subject.Chapter chapter : subject.getChapters()) {
                Document progress = progressByChapter.get(chapter.getChapterId());
                ChapterProgress cp = new ChapterProgress();
                cp.setProgressId(chapter.getChapterId());
                cp.setBatchId(batchId);
                cp.setChapterId(chapter.getChapterId());
                cp.setChapterName(chapter.getName());
                cp.setSubjectName(subject.getSubjectName());
                cp.setCompletionPercentage(readInt(progress, "completion", readInt(progress, "completion_percentage", 0)));
                cp.setRemarks(progress != null ? progress.getString("remarks") : "");
                cp.setStatus(cp.getCompletionPercentage() == 100 ? "Completed" : (cp.getCompletionPercentage() > 0 ? "In Progress" : "Not Started"));
                list.add(cp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateBatchSyllabusProgress(int batchId, int chapterId, int completion, String remarks, String updatedBy) {
        if (batchCollection == null) return false;
        try {
            Document progress = new Document()
                .append("chapter_id", chapterId)
                .append("completion", completion)
                .append("completion_percentage", completion)
                .append("remarks", remarks)
                .append("updated_by", updatedBy)
                .append("last_updated", new java.util.Date());

            long matched = batchCollection.updateOne(
                Filters.and(
                    Filters.eq("_id", batchId),
                    Filters.eq("syllabus_progress.chapter_id", chapterId)
                ),
                Updates.set("syllabus_progress.$", progress)
            ).getMatchedCount();

            if (matched == 0) {
                batchCollection.updateOne(Filters.eq("_id", batchId), Updates.push("syllabus_progress", progress));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int readInt(Document doc, String key, int fallback) {
        if (doc == null) return fallback;
        Object value = doc.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) {
            try { return Integer.parseInt(value.toString()); } catch (Exception ignored) {}
        }
        return fallback;
    }
}

package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.Batch;
import db.DBConnection;
import db.DocumentMapper;

public class BatchDAO {

    private MongoCollection<Document> batchCollection;

    public BatchDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            batchCollection = database.getCollection("batches");
        }
    }

    public boolean addBatch(Batch batch) {
        if (batchCollection == null) return false;
        try {
            Document doc = DocumentMapper.batchToDocument(batch);
            batchCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Batch getBatchById(int batchId) {
        if (batchCollection == null) return null;
        try {
            Document doc = batchCollection.find(Filters.eq("_id", batchId)).first();
            return DocumentMapper.documentToBatch(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Batch> getAllBatches() {
        List<Batch> list = new ArrayList<>();
        if (batchCollection == null) return list;

        try (MongoCursor<Document> cursor = batchCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Batch b = DocumentMapper.documentToBatch(doc);
                if (b != null) {
                    list.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Batch> getBatchesByTeacherId(String teacherId) {
        List<Batch> list = new ArrayList<>();
        if (batchCollection == null) return list;

        try (MongoCursor<Document> cursor = batchCollection.find(Filters.eq("teacher_id", teacherId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Batch b = DocumentMapper.documentToBatch(doc);
                if (b != null) {
                    list.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteBatch(int batchId) {
        if (batchCollection == null) return false;
        try {
            long deletedCount = batchCollection.deleteOne(Filters.eq("_id", batchId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

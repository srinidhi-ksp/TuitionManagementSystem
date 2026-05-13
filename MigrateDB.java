import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MigrateDB {
    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("tuitionManagementSystem");
            
            // Migrate Subjects
            MongoCollection<Document> subjects = db.getCollection("subjects");
            List<Document> allSubjects = subjects.find().into(new ArrayList<>());
            for (Document doc : allSubjects) {
                Object id = doc.get("_id");
                if (id instanceof String && ((String) id).startsWith("SUB")) {
                    String strId = (String) id;
                    int newId = Integer.parseInt(strId.replaceAll("\\D+", ""));
                    System.out.println("Migrating Subject: " + strId + " -> " + newId);
                    
                    Document newDoc = new Document(doc);
                    newDoc.put("_id", newId);
                    subjects.insertOne(newDoc);
                    subjects.deleteOne(new Document("_id", strId));
                }
            }

            // Migrate Batches
            MongoCollection<Document> batches = db.getCollection("batches");
            List<Document> allBatches = batches.find().into(new ArrayList<>());
            for (Document doc : allBatches) {
                Object id = doc.get("_id");
                boolean needsUpdate = false;
                Document newDoc = new Document(doc);

                if (id instanceof String && ((String) id).startsWith("B")) {
                    String strId = (String) id;
                    int newId = Integer.parseInt(strId.replaceAll("\\D+", ""));
                    System.out.println("Migrating Batch: " + strId + " -> " + newId);
                    newDoc.put("_id", newId);
                    needsUpdate = true;
                }
                
                Object subId = doc.get("subject_id");
                if (subId instanceof String && ((String) subId).startsWith("SUB")) {
                    int newSubId = Integer.parseInt(((String) subId).replaceAll("\\D+", ""));
                    newDoc.put("subject_id", newSubId);
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    if (id instanceof String && ((String) id).startsWith("B")) {
                        batches.insertOne(newDoc);
                        batches.deleteOne(new Document("_id", id));
                    } else {
                        batches.replaceOne(new Document("_id", id), newDoc);
                    }
                }
            }
            
            System.out.println("Migration complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

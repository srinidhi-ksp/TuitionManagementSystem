package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.Parent;
import db.DBConnection;
import db.DocumentMapper;

public class ParentDAO {

    private MongoCollection<Document> parentCollection;

    public ParentDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            parentCollection = database.getCollection("parents");
        }
    }

    public boolean addParent(Parent parent) {
        if (parentCollection == null) return false;
        try {
            Document doc = DocumentMapper.parentToDocument(parent);
            parentCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Parent getParentById(String userId) {
        if (parentCollection == null) return null;
        try {
            Document doc = parentCollection.find(Filters.eq("user_id", userId)).first();
            if (doc != null) {
                Parent p = DocumentMapper.documentToParent(doc);
                // Also get the associated user details to set name correctly if missing
                MongoDatabase database = DBConnection.getDatabase();
                if (database != null) {
                    MongoCollection<Document> usersColl = database.getCollection("users");
                    Document userDoc = usersColl.find(Filters.eq("_id", userId)).first();
                    if (userDoc != null) {
                        String fName = userDoc.getString("first_name");
                        String lName = userDoc.getString("last_name");
                        p.setName(((fName != null ? fName : "") + " " + (lName != null ? lName : "")).trim());
                    }
                }
                return p;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteParent(String userId) {
        if (parentCollection == null) return false;
        try {
            long deletedCount = parentCollection.deleteOne(Filters.eq("user_id", userId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Parent> getAllParents() {
        List<Parent> list = new ArrayList<>();
        if (parentCollection == null) return list;

        try (MongoCursor<Document> cursor = parentCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Parent p = DocumentMapper.documentToParent(doc);
                if (p != null) {
                    MongoDatabase database = DBConnection.getDatabase();
                    if (database != null) {
                        MongoCollection<Document> usersColl = database.getCollection("users");
                        Document userDoc = usersColl.find(Filters.eq("_id", p.getUserId())).first();
                        if (userDoc != null) {
                            String fName = userDoc.getString("first_name");
                            String lName = userDoc.getString("last_name");
                            p.setName(((fName != null ? fName : "") + " " + (lName != null ? lName : "")).trim());
                        }
                    }
                    list.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

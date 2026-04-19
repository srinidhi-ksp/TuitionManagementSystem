package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import model.Fee;
import model.Payment;
import db.DBConnection;
import db.DocumentMapper;

public class FeesDAO {
    private MongoCollection<Document> feesCollection;
    private MongoCollection<Document> paymentsCollection;

    public FeesDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            feesCollection = database.getCollection("fees");
            paymentsCollection = database.getCollection("payments");
        }
    }

    public Fee getFeeSummaryForStudent(String userId) {
        if (feesCollection == null) return null;
        try {
            Document doc = feesCollection.find(Filters.eq("user_id", userId)).first();
            return DocumentMapper.documentToFee(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Payment> getPaymentsForStudent(String userId) {
        List<Payment> list = new ArrayList<>();
        if (paymentsCollection == null || feesCollection == null) return list;

        try {
            // First find the fee doc for the student to get fee_id
            Document feeDoc = feesCollection.find(Filters.eq("user_id", userId)).first();
            if (feeDoc != null) {
                Object feeId = feeDoc.get("_id");
                
                try (MongoCursor<Document> cursor = paymentsCollection.find(Filters.eq("fee_id", feeId))
                        .sort(Sorts.descending("payment_date")).iterator()) {
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        Payment p = DocumentMapper.documentToPayment(doc);
                        if (p != null) {
                            list.add(p);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

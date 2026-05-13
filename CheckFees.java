import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.ArrayList;

public class CheckFees {
    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("tuitionManagementDB");
            System.out.println("Fees:");
            for(Document doc : db.getCollection("fees").find()) {
                System.out.println(doc.toJson());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}

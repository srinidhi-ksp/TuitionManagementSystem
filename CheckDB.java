import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.ArrayList;

public class CheckDB {
    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("tuitionManagementDB");
            System.out.println("Payments:");
            System.out.println(db.getCollection("payments").find().into(new ArrayList<Document>()));
            System.out.println("Enrollments:");
            System.out.println(db.getCollection("enrollments").find().into(new ArrayList<Document>()));
        } catch (Exception e) { e.printStackTrace(); }
    }
}

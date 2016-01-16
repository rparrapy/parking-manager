package nl.tue.iot.reservation;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.leshan.server.LwM2mServer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.mongodb.client.model.Filters.*;

public class BillsServlet extends HttpServlet {
    private final LwM2mServer server;
    public static MongoDatabase db;
    public static MongoClient mongoClient;

    public BillsServlet(LwM2mServer server) {
        this.server = server;
        this.mongoClient = new MongoClient("localhost", 27017);
        this.db = mongoClient.getDatabase("test");
    }



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String parkingSpotId = req.getParameter("parkingSpotId");
        String date = req.getParameter("date"); // format: ISO-8601 yyyy-mm-dd
        String path;

        MongoCollection<Document> coll = db.getCollection("reservations");
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS\'Z\'");
            FindIterable cursor = coll.find(and(gte("date", df.parse(date + "T00:00:00.000Z")), lte("date", df.parse(date + "T23:59:59.999Z"))));

            if (parkingSpotId == null) {
                path = "all-parking-spot-bills.json";
                Block<Document> printBlock = new Block<Document>() {
                    @Override
                    public void apply(final Document document) {


                        String parkingClientId = document.getString("parkingClientId");
                        String parkingSpotId = document.getString("parkingSpotId");
                        String vehicleId = document.getString("vehicleId");
                        String billingRate = document.getString("billingRate");
                        String action = document.getString("action");
                        Date time = document.getDate("time");

                        System.out.println(document.toJson());
                    }
                };
                cursor.forEach(printBlock);
            } else {
                path = "parking-spot-bills.json";
                Block<Document> printBlock = new Block<Document>() {
                    @Override
                    public void apply(final Document document) {


                        String parkingClientId = document.getString("parkingClientId");
                        String parkingSpotId = document.getString("parkingSpotId");
                        String vehicleId = document.getString("vehicleId");
                        String billingRate = document.getString("billingRate");
                        String action = document.getString("action");
                        Date time = document.getDate("time");

                        System.out.println(document.toJson());
                    }
                };
                cursor.forEach(printBlock);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        path = "parking-spot-bills.json";
        byte[] sampleResponse = Files.readAllBytes(Paths.get(path));
        resp.setContentType("application/json");
        resp.getOutputStream().write(sampleResponse);
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
    }
}

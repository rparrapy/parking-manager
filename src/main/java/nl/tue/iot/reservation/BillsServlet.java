package nl.tue.iot.reservation;
import com.google.gson.Gson;
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
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.*;

public class BillsServlet extends HttpServlet {
    private final LwM2mServer server;
    public static MongoDatabase db;
    public static MongoClient mongoClient;
    MongoCollection<Document> coll;


    public BillsServlet(LwM2mServer server) {
        this.server = server;
        this.mongoClient = new MongoClient("localhost", 27017);
        this.db = mongoClient.getDatabase("test");
        this.coll = db.getCollection("events");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String parkingSpotId = req.getParameter("parkingSpotId");
        String date = req.getParameter("date"); // format: ISO-8601 yyyy-mm-dd
        String path;
        FindIterable<Document> cursor;
        ArrayList ret = new ArrayList();
        System.out.println("-- " + date);
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS\'Z\'");
            Date from = df.parse(date + "T00:00:00.000Z");
            Date to =  df.parse(date + "T23:59:59.999Z");
            System.out.println("-- from " + from);
            System.out.println("-- to " + to);

            if (parkingSpotId == null) {
                cursor = coll.find(and(gte("time", from), lte("time", to)));
            } else {
                System.out.println("parkingSpotId" + parkingSpotId);
                cursor = coll.find(and(gte("time", from), lte("time", to), eq("parkingSpotId", parkingSpotId)));
            }

            for (Document document : cursor) {
                ret.add(document);
                System.out.println(document.toJson());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Gson gson = new Gson();
        String json = gson.toJson(ret);

        if (parkingSpotId == null) {
            path = "all-parking-spot-bills.json";
        } else {
            path = "parking-spot-bills.json";
        }
        //byte[] sampleResponse = Files.readAllBytes(Paths.get(path));
        byte[] content = json.getBytes();

        resp.setContentType("application/json");
        resp.getOutputStream().write(content);
        resp.setStatus(HttpServletResponse.SC_OK);
        return;
    }
}

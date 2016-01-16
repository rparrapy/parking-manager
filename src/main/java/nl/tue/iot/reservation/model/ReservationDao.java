package nl.tue.iot.reservation.model;
import java.util.Date;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

public class ReservationDao {
	
	private static MongoClient mongoClient = new MongoClient("localhost", 27017);
	private static MongoDatabase db = mongoClient.getDatabase("test");
    private static MongoCollection events = db.getCollection("events");

    public static void main(String[] args){
		
	}
	
	public static void writeEventToDatabase(String parkingClientId, String parkingSpotId,String vehicleId, Double billingRate, String action){


        Document lastEvent = (Document) events.find(eq("parkingSpotId", parkingSpotId)).sort(descending("_id")).first();

		Document document = new Document();
		document.put("parkingClientId",parkingClientId);
		document.put("parkingSpotId", parkingSpotId);

        if(vehicleId == null) {
            document.put("vehicleId", lastEvent.get("vehicleId"));
        } else {
            document.put("vehicleId", vehicleId);
        }

        if(billingRate == null) {
            document.put("billingRate", lastEvent.get("billingRate"));
        } else {
            document.put("billingRate", billingRate);
        }

		document.put("action",action);
		document.put("time", new Date());
		events.insertOne(document);
	}

    public static void closeConnection(){
		if(mongoClient != null){
			System.out.println("--- closing mongo db connection ---");
			mongoClient.close();
		}
	}

}

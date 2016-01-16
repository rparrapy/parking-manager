package nl.tue.iot.reservation.model;
import java.util.Date;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ReservationDao {
	
	public static MongoClient mongoClient = new MongoClient("localhost", 27017);
	public static MongoDatabase db = mongoClient.getDatabase("test");
	
	
	public static void main(String[] args){
		
	}
	
	public static void writeReservationToDatabase(String parkingClientId, String parkingSpotId,String vehicleId, double billingRate, String action){
		MongoCollection reservations = db.getCollection("reservations");
		Document document = new Document();
		document.put("parkingClientId",parkingClientId);
		document.put("parkingSpotId", parkingSpotId);
		document.put("vehicleId", vehicleId);
		document.put("billingRate",billingRate);
		document.put("action",action);
		document.put("time", new Date());
		reservations.insertOne(document);	
	}
	
	public static void closeConnection(){
		if(mongoClient != null){
			System.out.println("--- closing mongo db connection ---");
			mongoClient.close();
		}
	}

}

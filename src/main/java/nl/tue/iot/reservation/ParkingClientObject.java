package nl.tue.iot.reservation;

import java.util.ArrayList;
import java.util.List;

class ParkingClientObject {
    private String endPoint;
    private String latitude;
    private String longitude;
    private List<ParkingSpot> spotList = new ArrayList<ParkingSpot>();

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public List<ParkingSpot> getSpotList() {
        return spotList;
    }

    public void setSpotList(List<ParkingSpot> spotList) {
        this.spotList = spotList;
    }

}
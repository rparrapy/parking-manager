package nl.tue.iot.reservation;

class ParkingSpot {

    private String parkingSpotId;
    private String parkingSpotState;
    private String vehicleId;

    public String getParkingSpotId() {
        return parkingSpotId;
    }

    public void setParkingSpotId(String parkingSpotId) {
        this.parkingSpotId = parkingSpotId;
    }

    public String getParkingSpotState() {
        return parkingSpotState;
    }

    public void setParkingSpotState(String parkingSpotState) {
        this.parkingSpotState = parkingSpotState;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public ParkingSpot(String parkingSpotId, String parkingSpotState, String vehicleId) {
        this.parkingSpotId = parkingSpotId;
        this.parkingSpotState = parkingSpotState;
        this.vehicleId = vehicleId;
    }

}
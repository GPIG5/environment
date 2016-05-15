package drones;

public class CamRequest {
	private float latitude;
	private float longitude;
	
	public CamRequest(float lat, float lng) {
		latitude = lat;
		longitude = lng;
	}
	
	public float getLatitude() {
		return latitude;
	}
	
	public float getLongitude() {
		return longitude;
	}
}

package mesh;

/**
 * Created by hm649 on 16/05/16.
 */
public class Location {

    private float lat;
    private float lon;
    private float alt;

    public Location(float lat, float lon, float alt) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getAlt() {
        return alt;
    }

    public void setAlt(float alt) {
        this.alt = alt;
    }
}

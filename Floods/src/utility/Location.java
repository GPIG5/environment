package utility;

import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;

// import org.geotools.referencing

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

    public static Location fromOSGB(float northings, float eastings, float alt) {
        try {
            CRSAuthorityFactory crsfac = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
            // 27700 is the EPSG code for OSGB36
            CoordinateReferenceSystem osgbcrs = crsfac.createCoordinateReferenceSystem("27700");
            // 4326 is the EPSG code for WGS84 aka Lat-Lng
            CoordinateReferenceSystem wgs84crs = crsfac.createCoordinateReferenceSystem("4326");

            CoordinateOperation op = new DefaultCoordinateOperationFactory().createOperation(osgbcrs, wgs84crs);

            DirectPosition osgb = new GeneralDirectPosition(northings, eastings);
            DirectPosition latLng = op.getMathTransform().transform(osgb, osgb);
            return new Location((float) latLng.getOrdinate(0), (float) latLng.getOrdinate(1), alt);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Location)) {
            return false;
        }

        Location otherObj = (Location) obj;

        if (otherObj == this) {
            return true;
        }

        return otherObj.alt == this.alt && otherObj.lon == this.lon && otherObj.lat == this.lat;
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

    // http://stackoverflow.com/questions/25618207/what-distance-calculation-longitude-latitude-is-more-precise
    // http://stackoverflow.com/questions/35213445/how-can-i-find-3d-distance-between-lat-lon-alt-points
    public float distance3D(Location l1) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        geodeticCalculator.setStartingGeographicPoint(this.lon, this.lat);
        geodeticCalculator.setDestinationGeographicPoint(l1.getLon(), l1.getLat());
        float d = (float) geodeticCalculator.getOrthodromicDistance();
        // Square distance
        float d2 = d * d;
        // Add z distance.
        float dz = this.alt - l1.getAlt();
        float dz2 = dz * dz;
        d2 += dz2;
        return (float) Math.sqrt((double) d2);
    }
    
    public float distance(Location l1) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        geodeticCalculator.setStartingGeographicPoint(this.lon, this.lat);
        geodeticCalculator.setDestinationGeographicPoint(l1.getLon(), l1.getLat());
        return (float) geodeticCalculator.getOrthodromicDistance();
    }


    // http://stackoverflow.com/questions/4313618/convert-latitude-and-longitude-to-northing-and-easting-in-java
    public DirectPosition getOSGB() throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {
        CRSAuthorityFactory crsfac = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
        // 27700 is the EPSG code for OSGB36
        CoordinateReferenceSystem osgbcrs = crsfac.createCoordinateReferenceSystem("27700");
        // 4326 is the EPSG code for WGS84 aka Lat-Lng
        CoordinateReferenceSystem wgs84crs = crsfac.createCoordinateReferenceSystem("4326");

        CoordinateOperation op = new DefaultCoordinateOperationFactory().createOperation(wgs84crs, osgbcrs);

        DirectPosition latLng = new GeneralDirectPosition(this.lat, this.lon);
        DirectPosition osgb = op.getMathTransform().transform(latLng, latLng);
//        System.out.println("X: " + osgb.getOrdinate(0));
//        System.out.println("Y: " + osgb.getOrdinate(1));
        return osgb;
    }
}

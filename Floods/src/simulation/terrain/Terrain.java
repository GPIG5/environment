package simulation.terrain;

import org.opengis.geometry.DirectPosition;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import simulation.water.Cells;
import utility.Location;

/**
 * Wrapper class for ASCTerrain,
 */
public class Terrain {
    // Scaling factor to apply to produced terrain.
    static final float scale = 0.006f;
    ASCTerrain ater;
    Geometry g;
    int nrows;
    int ncols;

    public Terrain(Material mat, String data) {
        ater = new ASCTerrain(data);
        g = new Geometry("Terrain", ater);
        g.setMaterial(mat);
        g.scale(scale, scale, scale);
        g.setCullHint(CullHint.Never);
        nrows = ater.getRows();
        ncols = ater.getCols();
    }

    public Cells makeCells(String heightmap) {
        return new Cells(ater.getVertices(), nrows, ncols, ater.getCellsize(), scale, heightmap);
    }

    public int getRows() {
        return nrows;
    }

    public int getCols() {
        return ncols;
    }

    public Geometry getGeometry() {
        return g;
    }

    public Vector3f osgbTo3D(DirectPosition osgb, float alt) {
        Vector3f loc = new Vector3f(((float) osgb.getOrdinate(1) - ater.getYll()) * scale,
                3 * alt * scale,
                ((float) osgb.getOrdinate(0) - ater.getXll()) * scale);
        return loc;
    }

    public Location pointToLoc(Vector3f l) {
        // Convert to OSGB
        float invscale = 1 / scale;
        float eastings = invscale * l.x + ater.getYll();
        float northings = invscale * l.z + ater.getXll();
        return Location.fromOSGB(northings, eastings, invscale * (l.y / 3));
    }
    
    public float getScale() {
    	return scale;
    }
}

package simulation.terrain;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;

import simulation.water.Cells;

/**
 * Wrapper class for ASCTerrain,
 *
 */
public class Terrain {
	ASCTerrain ater;
	Geometry g;
	// Scaling factor to apply to produced terrain.
	static final float scale = 0.006f;
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
}

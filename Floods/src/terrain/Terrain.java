package terrain;


import java.util.ArrayList;
import java.util.PriorityQueue;

import com.jme3.material.Material;

import water.Cells;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Quad;

//http://environment.data.gov.uk/ds/survey/index.jsp
public class Terrain {
	ASCTerrain ater;
	Geometry g;
	static final float scale = 0.006f;
	int nrows;
	int ncols;
	
	public Terrain(Material mat) {
		ater = new ASCTerrain("lidar.zip");
		g = new Geometry("Terrain", ater);
        g.setMaterial(mat);
        g.scale(scale, scale, scale);
        g.setCullHint(CullHint.Never);
        nrows = ater.getRows();
        ncols = ater.getCols();
	}

	public Cells makeCells() {
		return new Cells(ater.getVertices(), nrows, ncols, ater.getCellsize(), scale);
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

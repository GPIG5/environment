package terrain;


import java.util.ArrayList;
import java.util.PriorityQueue;

import com.jme3.material.Material;

import water.Cell;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

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
        nrows = ater.getRows();
        ncols = ater.getCols();
	}

	public Cell[][] makeCells() {
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				
			}
		}
		return null;
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

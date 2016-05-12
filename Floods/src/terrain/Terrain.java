package terrain;


import java.util.ArrayList;
import java.util.PriorityQueue;

import com.jme3.material.Material;

import water.Cell;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
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
        nrows = ater.getRows();
        ncols = ater.getCols();
	}

	public Cell[] makeCells() {
		System.out.println("Making water cells...");
		Vector3f vertices[] = ater.getVertices();
		Cell[] result =  new Cell[(nrows-1)*(ncols-1)];
		float csize = ater.getCellsize() * scale;
		Quad q = new Quad(csize, csize);
		int i = 0;
		for (int r = 0; r < nrows-1; r++) {
			//System.out.println("Row: " + r);
			for (int c = 0; c < ncols-1; c++) {
				int base = (r * ncols) + c;
				Vector3f v1 = vertices[base].mult(scale);
				v1.addLocal(g.getWorldTranslation());
				Vector3f v2 = vertices[base + 1].mult(scale);
				v2.addLocal(g.getWorldTranslation());
				Vector3f v3 = vertices[base + ncols].mult(scale);
				v3.addLocal(g.getWorldTranslation());
				Vector3f v4 = vertices[base + ncols + 1].mult(scale);
				v4.addLocal(g.getWorldTranslation());
				result[i] = new Cell(v1, v2, v3, v4, csize);
				result[i].setMesh(q);
				i++;
			}
		}
		System.out.println("Making pipes...");
		// Run through again and set neighbours. 
		// n, e, s, w
		// Do corners.
		// TL
		result[0].setPipes(
				null, result[1], result[ncols-1], null);
		// TR
		result[ncols-2].setPipes(
				null, null, result[(ncols-1) + (ncols-2)], result[ncols-3]);
		// BL
		result[(ncols-1)*(nrows-2)].setPipes(
				result[(ncols-1)*(nrows-3)], result[(ncols-1)*(nrows-2) + 1], null, null);
		// BR
		result[(ncols-1)*(nrows-2) + (ncols-2)].setPipes(
				result[(ncols-1)*(nrows-3) + (ncols-2)], null, null, result[(ncols-1)*(nrows-2) + (ncols-3)]);
		// First + last row (not corners)
		for (int c = 1; c < ncols-2; c++) {
			// First row
			result[c].setPipes(
					null, result[c+1], result[(ncols-1) + c], result[c-1]);
			// Last row
			result[(ncols-1)*(nrows-2) + c].setPipes(
					result[(ncols-1)*(nrows-3) + c], result[(ncols-1)*(nrows-2) + (c+1)], null, result[(ncols-1)*(nrows-2) + (c-1)]);
		}
		// First + last column (not corners)
		for (int r = 1; r < nrows-2; r++) {
			// First col
			result[(ncols-1)*r].setPipes(
					result[(ncols-1)*(r-1)], result[(ncols-1)*r + 1], result[(ncols-1)*(r+1)], null);
			// Last col
			result[(ncols-1)*r + (ncols-2)].setPipes(
					result[(ncols-1)*(r-1) + (ncols-2)], null, result[(ncols-1)*(r+1) + (ncols-2)], result[(ncols-1)*r + (ncols-3)]);
			// Do centre.
			for (int c = 1; c < ncols-2; c++) {
				result[(ncols-1)*r + c].setPipes(
						result[(ncols-1)*(r-1) + c], result[(ncols-1)*r + (c+1)], result[(ncols-1)*(r+1) + c], result[(ncols-1)*r + (c-1)]);
			}
		}
		return result;
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

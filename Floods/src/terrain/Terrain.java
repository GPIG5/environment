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
		Vector3f vertices[] = ater.getVertices();
		Cell[][] result =  new Cell[nrows-1][ncols-1];
		float csize = ater.getCellsize();
		for (int r = 0; r < nrows-1; r++) {
			for (int c = 0; c < ncols-1; c++) {
				int base = (r * ncols) + c;
				Vector3f v1 = vertices[base].mult(g.getWorldScale());
				v1.addLocal(g.getWorldTranslation());
				Vector3f v2 = vertices[base + 1].mult(g.getWorldScale());
				v2.addLocal(g.getWorldTranslation());
				Vector3f v3 = vertices[base + ncols].mult(g.getWorldScale());
				v3.addLocal(g.getWorldTranslation());
				Vector3f v4 = vertices[base + ncols + 1].mult(g.getWorldScale());
				v4.addLocal(g.getWorldTranslation());
				result[r][c] = new Cell(v1, v2, v3, v4, csize);
			}
		}
		// Run through again and set neighbours. 
		// nw, n, ne, e, se, s, sw, w
		// Do corners.
		// TL
		result[0][0].setPipes(
				null, null, null, 
				result[0][1],
				result[1][1], result[1][0], null,
				null);
		// TR
		result[0][ncols-2].setPipes(
				null, null, null, 
				null,
				null, result[1][ncols-2], result[1][ncols-3],
				result[0][ncols-3]);
		// BL
		result[nrows-2][0].setPipes( 
				null, result[nrows-3][0], result[nrows-3][1], 
				result[nrows-2][1],
				null, null, null,
				null);
		// BR
		result[nrows-2][ncols-2].setPipes( 
				result[nrows-3][ncols-3], result[nrows-3][ncols-2], null, 
				null,
				null, null, null,
				result[nrows-2][ncols-3]);
		// First + last row (not corners)
		for (int c = 1; c < ncols-2; c++) {
			// First row
			result[0][c].setPipes(
					null, null, null, 
					result[0][c+1], 
					result[1][c+1], result[1][c], result[1][c-1], 
					result[0][c-1]);
			// Last row
			result[nrows-2][c].setPipes(
					result[nrows-3][c-1], result[nrows-3][c], result[nrows-3][c+1], 
					result[nrows-2][c+1],
					null, null, null, 
					result[nrows-2][c-1]);
		}
		// First + last column (not corners)
		for (int r = 1; r < nrows-2; r++) {
			// First col
			result[r][0].setPipes(
					null, result[r-1][0], result[r-1][1], 
					result[r][1],
					result[r+1][1], result[r+1][0], null,
					null);
			// Last col
			result[r][ncols-2].setPipes(
					result[r-1][ncols-3], result[r-1][ncols-2], null,
					null,
					null, result[r+1][ncols-2], result[r+1][ncols-3],
					result[r][ncols-3]);
			// Do centre.
			for (int c = 1; c < ncols-2; c++) {
				result[r][c].setPipes(
						result[r-1][c-1], result[r-1][c], result[r-1][c+1], 
						result[r][c+1],
						result[r+1][c+1], result[r+1][c], result[r+1][c-1], 
						result[r][c-1]);
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

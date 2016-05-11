package water;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

// Hydrostatic pipe model.
// Dynamic Simulation of Splashing Fluids - O'Brien and Hodgins
// Interactive Terrain Modeling Using Hydraulic Erosion - Stava, Benes, Brisbin

public class Cell extends Geometry {
	// Each cell has pipes diagonally vertically and horizontally.
	// These are clockwise from NW
	private Cell[] pipes;
	private Vector3f[] points;
	// Volume of the base
	private float basevol;
	// The min height which rendering starts at.
	private float minh;
	private float cellsize;
	
	public Cell(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float csize) {
		points = new Vector3f[] {p0, p1, p2, p3};
		pipes = new Cell[8];
		cellsize = csize;
		float min0 = p0.y;
		float max0 = p0.y;
		// min,max y of a,b,c
		for (int i = 1; i < 3; i++) {
			if (points[i].y < min0) {
				min0 = points[i].y;
			}
			if (points[i].y > max0) {
				max0 = points[i].y;
			}
		}
		// min,max y of b,c,d
		float min1 = p1.y;
		float max1 = p1.y;
		for (int i = 2; i < 4; i++) {
			if (points[i].y < min1) {
				min1 = points[i].y;
			}
			if (points[i].y > max1) {
				max1 = points[i].y;
			}
		}
		if (max0 > max1) {
			minh = max0;
		}
		else {
			minh = max1;
		}
		// Calc base vol.
		basevol = (max0 - min0) + (max1 - min1);
		basevol *= (csize * csize)/4;
	}
	
	public void setPipes(Cell nw, Cell n, Cell ne, Cell e, Cell se, Cell s, Cell sw, Cell w) {
		pipes[0] = nw;
		pipes[1] = n;
		pipes[2] = ne;
		pipes[3] = e;
		pipes[4] = se;
		pipes[5] = s;
		pipes[6] = sw;
		pipes[7] = w;
	}
}

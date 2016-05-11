package water;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

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
	// This is the rendered height.
	private static float height = 4;
	// Gravity.
	private static final float g = 0.0f;
	// Fluid density.
	private static final float rho = 0.0f;
	// Atmospheric pressure.
	private static final float p0 = 0.0f;
	// pipe length.
	private static final float l = 1f;
	private static final float c = 1f;
	// pipe cross sectional area
	// centre x and z values;
	private float avgx;
	private float avgz;
	private float cellsize;
	private float flows[];
	
	public Cell(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float csize) {
		// Rendering things.
		Box b =  new Box(csize/2,0.4f,csize/2);
		setMesh(b);
		//this.scale(1,height,1);
		// Everything important.
		points = new Vector3f[] {p0, p1, p2, p3};
		pipes = new Cell[8];
		flows = new float[8];
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
		// Rendering things.
		avgz = (p0.z + p1.z + p2.z + p3.z)/4;
		avgx = (p0.x + p1.x + p2.x + p3.x)/4;
		// this moves the centre.
		this.move(new Vector3f(avgx, minh+0.4f, avgz));
	}
	
	public void test() {
		// Cull if we have no water.
		if (this.height < minh) {
			setCullHint(CullHint.Always);
		}
	}
	
	public void calc() {
		// static pressure
		float h = (height * rho * g) + p0;
		float volume = basevol + cellsize * cellsize * height;
	}
	
	public float getHeight() {
		return height;
	}
	
	public void render(float t) {
		float pg = rho*g;
		float pl = rho * l;
		float sum = 0.0f;
		for (int p = 0; p < 8; p++) {
			if (pipes[p] != null) {
				float a = (pg * (height - pipes[p].getHeight()))/pl;
				float flow = flows[p] + t*c*a;
				sum += (flow + flows[p])/2;
				// Update previous flow.
				flows[p] = flow;
			}
		}
		float deltav = t * sum;
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

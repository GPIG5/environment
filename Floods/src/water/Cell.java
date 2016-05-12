package water;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

// Hydrostatic pipe model.
// Dynamic Simulation of Splashing Fluids - O'Brien and Hodgins
// Interactive Terrain Modeling Using Hydraulic Erosion - Stava, Benes, Brisbin
// http://wiki.lwjgl.org/wiki/OpenCL_in_LWJGL
public class Cell extends Geometry {
	// Each cell has pipes diagonally vertically and horizontally.
	// These are clockwise from N
	private Cell[] pipes;
	private Vector3f[] points;
	// Volume of the base
	private float basevol;
	// The min height which rendering starts at.
	private float minh;
	// This is the volume of water.
	private float volume = 1;
	// This is the height.
	private float height;
	// Gravity.
	private static final float g = 9.81f;
	// Fluid density.
	private static final float rho = 1.0f;
	// Atmospheric pressure.
	private static final float p0 = 1f;
	// pipe length.
	private static final float l = 1f;
	private static final float c = 1f;
	// flow factor
	private static final float flowf = 0.00005f * g / l;
	private static final int flowLUT[] = {2,3,0,1}; 
	// pipe cross sectional area
	// centre x and z values;
	private float avgx;
	private float avgz;
	private float cellsize;
	private float flows[];
	
	public Cell(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float csize) {
		// Rendering things.
		//this.scale(1,height,1);
		// Everything important.
		points = new Vector3f[] {p0, p1, p2, p3};
		pipes = new Cell[4];
		flows = new float[]{0,0,0,0};
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
		basevol *= (csize * csize)/4.0f;
		volume = basevol*0.25f;
		// Rendering things.
		avgz = (p0.z + p1.z + p2.z + p3.z)/4;
		avgx = (p0.x + p1.x + p2.x + p3.x)/4;
		// this moves the centre.
		height = calcHeight();
		this.rotate(-FastMath.HALF_PI, 0, 0);
		this.move(p1.x, height, p1.z);
	}
	
	public void test() {
		// Cull if we have no water.
		if (this.height < minh) {
			setCullHint(CullHint.Always);
		}
	}

	// Calculate height from volume.
	private float calcHeight() {
		return ((volume - basevol)/(cellsize*cellsize)) + minh;
	}
	
	public float getHeight() {
		return height;
	}
	
	public void setVoume(float v) {
		volume = v;
	}
	
	// https://github.com/karhu/terrain-erosion/blob/master/Simulation/FluidSimulation.cpp
	public void process(float t) {
		float flowfactor = t * flowf;
		float sum = 0.0f;
		float nflow;
		
		// n
		if (pipes[0] != null) {
			nflow = flows[0] + flowfactor * (height - pipes[0].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[0] = nflow;
			sum += nflow;
		}
		// e
		if (pipes[1] != null) {
			nflow = flows[1] + flowfactor * (height - pipes[1].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[1] = nflow;
			sum += nflow;
		}
		// s
		if (pipes[2] != null) {
			nflow = flows[2] + flowfactor * (height - pipes[2].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[2] = nflow;
			sum += nflow;
		}
		// w
		if (pipes[3] != null) {
			nflow = flows[3] + flowfactor * (height - pipes[3].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[3] = nflow;
			sum += nflow;
		}
		/*
		//System.out.println("Sum: " + sum);
		float deltav = t * sum;
		volume += deltav;
		// clamp volume
		if (volume < 0) {
			volume = 0.0f;
		} */
	}
	
	public float getFlow(int p) {
		return flows[p];
	}
	
	public void redraw(float t) {
		float inflow = 0.0f;
		float outflow = 0.0f;
		// n
		if (pipes[0] != null) {
			inflow += pipes[0].getFlow(2);
			outflow += flows[0];
		}
		// e
		if (pipes[1] != null) {
			inflow += pipes[1].getFlow(3);
			outflow += flows[1];
		}
		// s
		if (pipes[2] != null) {
			inflow += pipes[2].getFlow(0);
			outflow += flows[2];
		}
		// w
		if (pipes[3] != null) {
			inflow += pipes[3].getFlow(1);
			outflow += flows[3];
		}
		float dv = t * (inflow-outflow);
		volume += dv;
		float nh = calcHeight();
		if (nh < minh) {
			this.setCullHint(cullHint.Always);
		}
		else {
			this.setCullHint(cullHint.Dynamic);
		}
		move(0,nh-height,0);
		height = nh;
	}
	
	public void setPipes(Cell n, Cell e, Cell s, Cell w) {
		pipes[0] = n;
		pipes[1] = e;
		pipes[2] = s;
		pipes[3] = w;
	}
}

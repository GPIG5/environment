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
	private Vector3f point;
	private Cell[] pipes;
	// Volume of the base (below terh).
	private float basevol;
	// The  height of the terrain.
	private float terh;
	// This is the height.
	private float waterh;
	// Gravity.
	private static final float g = 9.81f;
	// pipe length.
	private static final float l = 1f;
	// flow factor
	private static final float flowf = 0.00005f * g / l;
	private float cellsize2;
	private float flows[];
	
	public Cell(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float csize) {
		pipes = new Cell[4];
		flows = new float[]{0,0,0,0};
		cellsize2 = csize*csize;
		// min,max y of a,b,c
		float min0 = p0.y;
		float max0 = p0.y;
		if (p1.y < min0) {
			min0 = p1.y;
		}
		if (p1.y > max0) {
			max0 = p1.y;
		}
		if (p2.y < min0) {
			min0 = p2.y;
		}
		if (p2.y > max0) {
			max0 = p2.y;
		}
		// min,max y of b,c,d
		float min1 = p1.y;
		float max1 = p1.y;
		if (p2.y < min1) {
			min1 = p2.y;
		}
		if (p2.y > max1) {
			max1 = p2.y;
		}
		if (p3.y < min1) {
			min1 = p3.y;
		}
		if (p3.y > max1) {
			max1 = p3.y;
		}
		// terrain height is height of max point.
		if (max0 > max1) {
			terh = max0;
		}
		else {
			terh = max1;
		}
		// Calc base vol.
		basevol = (max0 - min0) + (max1 - min1);
		basevol *= (csize * csize)/4.0f;
		// Rendering things.
		rotate(-FastMath.HALF_PI, 0, 0);
		point = p1;
		move(p1.x, terh, p1.z);
		//setCullHint(cullHint.Always);
	}
	
	public float getHeight() {
		return waterh + terh;
	}
	
	// add water to this cell.
	public void add(float v) {
		waterh += v;
	}
	
	// https://github.com/karhu/terrain-erosion/blob/master/Simulation/FluidSimulation.cpp
	public void flow(float t) {
		float flowfactor = t * flowf;
		float sum = 0.0f;
		float nflow;
		// n
		if (pipes[0] != null) {
			nflow = flows[0] + flowfactor * (waterh - pipes[0].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[0] = nflow;
			sum += nflow;
		}
		// e
		if (pipes[1] != null) {
			nflow = flows[1] + flowfactor * (waterh - pipes[1].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[1] = nflow;
			sum += nflow;
		}
		// s
		if (pipes[2] != null) {
			nflow = flows[2] + flowfactor * (waterh - pipes[2].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[2] = nflow;
			sum += nflow;
		}
		// w
		if (pipes[3] != null) {
			nflow = flows[3] + flowfactor * (waterh - pipes[3].getHeight());
			if (nflow < 0) {
				nflow = 0;
			}
			flows[3] = nflow;
			sum += nflow;
		}
		// Scale
		// -basevol?
//		float k = Float.min(1.0f, (waterh*cellsize2)/(sum*t));
//		flows[0] *= k;
//		flows[1] *= k;
//		flows[2] *= k;
//		flows[3] *= k;
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
		waterh += dv/(cellsize2);
		waterh = Float.max(0.0f, waterh);
		if (waterh == 0) {
			this.setCullHint(cullHint.Always);
		}
		else {
			setCullHint(cullHint.Dynamic);
			setLocalTranslation(point.x, waterh+terh, point.z);
		}
	}
	
	public void setPipes(Cell n, Cell e, Cell s, Cell w) {
		pipes[0] = n;
		pipes[1] = e;
		pipes[2] = s;
		pipes[3] = w;
	}
}

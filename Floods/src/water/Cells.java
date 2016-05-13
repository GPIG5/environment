package water;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

// Wrapper class for cell things.
public class Cells {
	float[] basevols;
	float[] terhs;
	float[] heights;
	float[] flows;
	FloatBuffer vbuffer;
	IntBuffer ebuffer;
	int[] pipes;
	float csize;
	float csize2;
	int rows;
	int cols;
	
	public Cells(Vector3f[] vertices, int nrows, int ncols, float cellsize, float scale) {
		System.out.println("Making water cells...");
		// Some much used values.
		Vector3f avg;
		int nr1 = nrows - 1;
		rows = nr1;
		int nc1 = ncols - 1;
		cols = nc1;
		int nr2 = nrows - 2;
		int nc2 = ncols - 2;
		
		int nr3 = nrows - 3;
		int nc3 = ncols - 3;
		csize = cellsize * scale;
		csize2 = csize * csize;
		// Make arrays.
		vbuffer = BufferUtils.createFloatBuffer(4*3*nr1*nc1);
		ebuffer = BufferUtils.createIntBuffer(6*nr1*nc1);
		basevols = new float[nr1*nc1];
		terhs =  new float[nr1*nc1];
		heights = new float[nr1*nc1];
		// 4 flows and pipes per cell.
		flows = new float[nr1*nc1*4];
		pipes = new int[nr1*nc1*4];
		// Make planes
		int i = 0;
		for (int r = 0; r < nr1; r++) {
			//System.out.println("Row: " + r);
			for (int c = 0; c < nc1; c++) {
				int base = (r * ncols) + c;
				Vector3f p0 = vertices[base].mult(scale);
				Vector3f p1 = vertices[base + 1].mult(scale);
				Vector3f p2 = vertices[base + ncols].mult(scale);
				Vector3f p3 = vertices[base + ncols + 1].mult(scale);
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
				//points[i] = new Vector3f(p1.x, 0, p1.z);
				if (min0 < min1) {
					terhs[i] = min0;
				}
				else {
					terhs[i] = min1;
				}
				//points[i].y = terhs[i];
				// Calc base vol.
				basevols[i] = (max0 - min0) + (max1 - min1);
				basevols[i] *= csize2/4.0f;
//				avg = p0.add(p1);
//				avg.addLocal(p2);
//				avg.addLocal(p3);
//				avg.multLocal(0.25f);
				vbuffer.put(p0.x);
				vbuffer.put(terhs[i]);
				vbuffer.put(p0.z);
				
				vbuffer.put(p1.x);
				vbuffer.put(terhs[i]);
				vbuffer.put(p1.z);
				
				vbuffer.put(p2.x);
				vbuffer.put(terhs[i]);
				vbuffer.put(p2.z);
				
				vbuffer.put(p3.x);
				vbuffer.put(terhs[i]);
				vbuffer.put(p3.z);
				
				ebuffer.put((i<<2));
				ebuffer.put((i<<2) + 1);
				ebuffer.put((i<<2) + 2);
				
				ebuffer.put((i<<2) + 1);
				ebuffer.put((i<<2) + 3);
				ebuffer.put((i<<2) + 2);
				i++;
			}
		}
		System.out.println("Making pipes...");
		int idx;
		// Run through again and set neighbours/pipes. 
		// n, e, s, w
		// Do corners.
		// TL (0,0)
		pipes[0] = -1;
		pipes[1] = 1;
		pipes[2] = nc1;
		pipes[3] = -1;
		// TR
		idx = (nc2)<<2;
		pipes[idx] = -1;
		pipes[idx+1] = -1;
		pipes[idx+2] = nc1 + nc2;
		pipes[idx+3] = nc3;
		// BL
		idx = (nc1*nr2)<<2;
		pipes[idx] = nc1*nr3;
		pipes[idx+1] = nc1*nr3+1;
		pipes[idx+2] = -1;
		pipes[idx+3] = -1;
		// BR
		idx = (nc1*nr2 + nc2)<<2;
		pipes[idx] = nc1*nr3 + nc2;
		pipes[idx+1] = -1;
		pipes[idx+2] = -1;
		pipes[idx+3] = nc1*nr2 + nc3;
		// First + last row (not corners)
		for (int c = 1; c < nc2; c++) {
			// First row
			idx = c<<2;
			pipes[idx] = -1;
			pipes[idx+1] = c+1;
			pipes[idx+2] = nc1 + c;
			pipes[idx+3] = c-1;
			// Last row
			idx = (nc1*nr2 + c)<<2;
			pipes[idx] = nc1*nr3 + c;
			pipes[idx+1] = nc1*nr2 + c + 1;
			pipes[idx+2] = -1;
			pipes[idx+3] = nc1*nr2 + c - 1;
		}
		// First + last column (not corners)
		for (int r = 1; r < nr2; r++) {
			// First col
			idx = (nc1*r)<<2;
			pipes[idx] = nc1*(r-1);
			pipes[idx+1] = nc1*r + 1;
			pipes[idx+2] = nc1*(r+1);
			pipes[idx+3] = -1;
			// Last col
			idx = (nc1*r + nc2)<<2;
			pipes[idx] = nc1*(r-1) + nc2;
			pipes[idx+1] = -1;
			pipes[idx+2] = nc1*(r+1) + nc2;
			pipes[idx+3] = nc1*r + nc3;
			// Do centre.
			for (int c = 1; c < nc2; c++) {
				idx = (nc1*r + c)<<2;
				pipes[idx] = nc1*(r-1) + c;
				pipes[idx+1] = nc1*r + c + 1;
				pipes[idx+2] = nc1*(r+1) + c;
				pipes[idx+3] = nc1*r + c - 1;
			}
		}
		vbuffer.rewind();
		ebuffer.rewind();
	}
	
	public int getSize() {
		return heights.length;
	}
	
	public float[] getHeights() {
		heights[300] = 20f;
		return heights;
	}
	
	public int[] getPipes() {
		return pipes;
	}
	
	public float[] getFlows() {
		return flows;
	}
	
	public float getCsize2() {
		return csize2;
	}
	
	public float[] getTerHeights() {
		return terhs;
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getCols() {
		return cols;
	}
	
	public FloatBuffer getVertices() {
		return vbuffer;
	}
	
	public IntBuffer getEdges() {
		return ebuffer;
	}
}

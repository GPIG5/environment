package water;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

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
		csize = cellsize * scale;
		csize2 = csize * csize;
		// Make arrays.
		vbuffer = BufferUtils.createFloatBuffer(4*3*nr1*nc1);
		ebuffer = BufferUtils.createIntBuffer(6*nr2*nc2);
		basevols = new float[nr1*nc1];
		terhs =  new float[nr1*nc1];
		heights = new float[nr1*nc1];
		// 4 flows and pipes per cell.
		flows = new float[nr1*nc1*4];
		// Load water height map + process
		try {
			BufferedImage img = ImageIO.read(Cells.class.getResourceAsStream("/assets/Textures/mask.png"));
			// Make planes
			int i = 0;
			System.out.println("Water is : "+nr1+"x"+nc1);
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
					avg = p0.add(p1);
					avg.addLocal(p2);
					avg.addLocal(p3);
					avg.multLocal(0.25f);
					vbuffer.put(avg.x);
					vbuffer.put(terhs[i]);
					vbuffer.put(avg.z);
					// Water heights
					int color = img.getRGB(c, nr2-r);
					if ((color & 0xFF0000) != 0) {
						// Red - depest
						heights[i] = 0.07f;
					}
					else if ((color & 0xFF00) != 0) {
						// Green
						heights[i]= 0.02f;
					}
					else if ((color & 0xFF) != 0) {
						heights[i] = 0.005f;
					}
					i++;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Making edges...");
		for (int r = 0; r < nr2; r++) {
			for (int c = 0; c < nc2; c++) {
				int base = (r * nc1) + c;
				ebuffer.put(base);
				ebuffer.put(base + 1);
				ebuffer.put(base + nc1);
				
				ebuffer.put(base + 1);
				ebuffer.put(base + nc1 + 1);
				ebuffer.put(base + nc1);
			}
		}
		vbuffer.rewind();
		ebuffer.rewind();
	}
	
	public int getSize() {
		return heights.length;
	}
	
	public float[] getHeights() {
		return heights;
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

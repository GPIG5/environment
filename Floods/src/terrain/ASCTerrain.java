package terrain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class ASCTerrain extends Mesh{
	int ncols, nrows;
	float nodata, cellsize;
	ArrayList<Float> points;
	
	float data[][];
	Triangle faces[][][];
	Vector3f fnormals[][][];
	// Actual data
	Vector3f normals[];
	Vector3f vertices[];
	Vector2f texcoord[];
	int indexes[];
	
	public ASCTerrain(String zfile) {
		int count;
		try {
			ZipInputStream zis = new ZipInputStream(ASCTerrain.class.getResourceAsStream("/data/"+zfile));
			ZipEntry ze = zis.getNextEntry();
			System.out.println("Opening file: " + ze.getName());
			byte[] asc = new byte[(int) ze.getSize()];
			count = 0;
			while (count < ze.getSize()) {
				count += zis.read(asc, count, (int) (ze.getSize() - count));
			}
			readFile(asc);
			zis.closeEntry();
			zis.close();
			process();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readFile(byte[] data) {
		int pStart = 0;
		int xllcorner = -1, yllcorner = -1;
		points =  new ArrayList<Float>();
		
		for (int pEnd = 0; pEnd < data.length; pEnd++) {
			if (data[pEnd] == '\n' || pEnd == (data.length - 1)) {
				String line = new String(data, pStart, pEnd-pStart);
				if (line.startsWith("ncols")) {
					ncols = Integer.parseInt(line.substring(5).trim());
				}
				else if (line.startsWith("nrows")) {
					nrows = Integer.parseInt(line.substring(5).trim());
				}
				else if (line.startsWith("xllcorner")) {
					xllcorner = (int) Float.parseFloat(line.substring(9).trim());
				}
				else if (line.startsWith("yllcorner")) {
					yllcorner = (int) Float.parseFloat(line.substring(9).trim());
				}
				else if (line.startsWith("cellsize")) {
					cellsize = Float.parseFloat(line.substring(8).trim());
				}
				else if (line.startsWith("NODATA_value")) {
					nodata = Float.parseFloat(line.substring(12).trim());
				}
				else {
					// Assume data
					line = line.trim();
					String[] parts = line.split("\\s+");
					for (String part : parts) {
						points.add(Float.parseFloat(part.trim()));
					}
				}
				pStart = pEnd + 1;
			}
		}
	}
	
	private void process() {
		downsample(30);
		vertices = new Vector3f[nrows*ncols];
		texcoord = new Vector2f[nrows*ncols];
		indexes = new int[6*(nrows-1)*(ncols-1)];
		//faces = new Triangle[nrows - 1][ncols-1][2];
		fnormals = new Vector3f[nrows-1][ncols-1][2];
		normals = new Vector3f[nrows*ncols];
		float x, z;
		
		System.out.println("Processing " + nrows + "x" + ncols + " terrain.");
		
		// First make vertices + texcoords
		x = 0.0f;
		for (int row = 0; row < nrows; row++) {
			z = 0.0f;
			for (int col = 0; col < ncols; col++) {
				vertices[(row * ncols) + col] = new Vector3f(x, data[row][col], z);
				texcoord[(row * ncols) + col] = new Vector2f(col/(float)ncols, row/(float)nrows);
				z += cellsize;
			}
			x += cellsize;
		}
		// Indexes + face normals
		System.out.println("Populating faces...");
		for (int row = 0; row < (nrows - 1); row++) {
			for (int col = 0; col < (ncols - 1); col++) {
					int base = 6*((row*(ncols-1)) + col);
					// Triangle 1
					indexes[base] = (row * ncols) + col; //a
					indexes[base+1] = (row * ncols) + col + 1; //b
					indexes[base+2] = ((row+1) * ncols) + col; //c
					// Triangle 2
					indexes[base+3] = ((row+1) * ncols) + col; //c
					indexes[base+4] = (row * ncols) + col + 1; //b
					indexes[base+5] = ((row+1) * ncols) + col + 1; //d
					// Faces
					base = (row * ncols) + col;
					Vector3f a = vertices[base];
					Vector3f b = vertices[base + 1];
					Vector3f c = vertices[base + ncols];
					Vector3f d = vertices[base + ncols + 1];
					Vector3f ba = b.subtract(a);
					Vector3f ca = c.subtract(a);
					Vector3f bc = b.subtract(c);
					Vector3f dc = d.subtract(c);
					//ormal.subtractLocal(pointa).crossLocal(pointc.x - pointa.x, pointc.y - pointa.y, pointc.z - pointa.z);
					fnormals[row][col][0] = ba.crossLocal(ca).normalizeLocal();
					fnormals[row][col][1] = bc.crossLocal(dc).normalizeLocal();
					//Triangle ta = new Triangle(a,b,c);
					//Triangle tb = new Triangle(c,b,d);
			}
		}
		System.out.println("Calculating face normals...");
		// Vector normals
		Vector3f t;
		// Corners
		// TL
		t = fnormals[0][0][0].add(fnormals[0][0][1]);
		t.normalizeLocal();
		normals[0] = t;
		// TR
		t = fnormals[0][ncols-2][0].add(fnormals[0][ncols-2][1]);
		t.normalizeLocal();
		normals[ncols-1] = t;
		// BL
		t = fnormals[nrows-2][0][0].add(fnormals[nrows-2][0][1]);
		t.normalizeLocal();
		normals[(nrows-1)*ncols] = t;
		// BR
		t = fnormals[nrows-2][ncols-2][0].add(fnormals[nrows-2][ncols-2][1]);
		t.normalizeLocal();
		normals[(nrows*ncols) - 1] = t;
		// Do first row and last row
		for (int col = 1; col < (ncols - 1); col++) {
			// First row
			t = fnormals[0][col-1][0].add(fnormals[0][col-1][1]);
			t.addLocal(fnormals[0][col][0]);
			t.addLocal(fnormals[0][col][1]);
			t.normalizeLocal();
			normals[col] = t;
			// Last row
			t = fnormals[nrows-2][col-1][0].add(fnormals[nrows-2][col-1][1]);
			t.addLocal(fnormals[nrows-2][col][0]);
			t.addLocal(fnormals[nrows-2][col][1]);
			t.normalizeLocal();
			normals[((nrows-1)*ncols) + col] = t;
		}
		// Do first and last column
		for (int row = 1; row < (nrows - 1); row++) {
			// First col
			t = fnormals[row-1][0][0].add(fnormals[row-1][0][1]);
			t.addLocal(fnormals[row][0][0]);
			t.addLocal(fnormals[row][0][1]);
			t.normalizeLocal();
			normals[row*ncols] = t;
			// Last col
			t = fnormals[row-1][ncols-2][0].add(fnormals[row-1][ncols-2][1]);
			t.addLocal(fnormals[row][ncols-2][0]);
			t.addLocal(fnormals[row][ncols-2][1]);
			t.normalizeLocal();
			normals[(row*ncols) + (ncols-1)] = t;
			// Centre cols
			for (int col = 1; col < (ncols - 1); col++) {
				t = fnormals[row-1][col-1][0].add(fnormals[row-1][col-1][1]);
				t.addLocal(fnormals[row-1][col][0]);
				t.addLocal(fnormals[row-1][col][1]);
				t.addLocal(fnormals[row][col-1][0]);
				t.addLocal(fnormals[row][col-1][1]);
				t.addLocal(fnormals[row][col][0]);
				t.addLocal(fnormals[row][col][1]);
				t.normalizeLocal();
				normals[(row*ncols) + col] = t;
			}
		}
		System.out.println("Processing finished...");
		setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texcoord));
		setBuffer(VertexBuffer.Type.Index,    3, BufferUtils.createIntBuffer(indexes));
		setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
		updateBound();
        System.out.println("NE corner: " + vertices[vertices.length - 1].mult(0.006f));
	}
	
	private void downsample(int nsize) {
		cellsize = cellsize * nsize;
		int newrows = nrows / nsize;
		int newcols = ncols / nsize;
		data = new float[newrows][newcols];
		for (int row = 0; row < newrows; row++) {
			for (int col = 0; col < newcols; col++) {
				float total = 0;
				// Sum neighbourhood
				for (int trow = 0; trow < nsize; trow++) {
					// Base for this row.
					int brow = (((row * nsize) + trow) * ncols) + (col * nsize);
					for (int tcol = 0; tcol < nsize; tcol++) {
						total += points.get(brow + tcol);
					}
				}
				total *= 3.0;
				data[newrows - row - 1][col] = total / (nsize * nsize);
			}
		}
		nrows = newrows;
		ncols = newcols;
	}
	
	public Vector3f[] getNormals() {
		return normals;
	}
	
	public Vector3f[][][] getFaceNormals() {
		return fnormals;
	}
	
	public Vector3f[] getVertices() {
		return vertices;
	}
	
	public Vector2f[] getTextureMapping() {
		return texcoord;
	}
	
	// Counter-clockwise!
	public int[] getIndexes() {
		return indexes;
	}
	
	public int getCols() {
		return ncols;
	}
	
	public int getRows() {
		return nrows;
	}
	
	public float getCellsize() {
		return cellsize;
	}
}

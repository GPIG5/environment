package simulation.terrain;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ASCTerrain extends Mesh {
    int xll, yll;
    int ncols, nrows;
    float nodata, cellsize;
    float data[][];
    // Actual data
    Vector3f normals[];
    Vector3f vertices[];
    FloatBuffer texcoord;
    IntBuffer indexes;

    /**
     * Constructs a new ASCTerrain, extends Mesh. Performs downsampling and
     * generation of normals.
     *
     * @param zfile The zip file containing the ASC (ARC/INFO ASCII Grid) file for
     *              height map data.
     */
    public ASCTerrain(String zfile) {
        int count;
        try {
            ZipInputStream zis = new ZipInputStream(ASCTerrain.class.getResourceAsStream("/assets/Terrain/" + zfile));
            ZipEntry ze = zis.getNextEntry();
            System.out.println("Opening file: " + ze.getName());
            byte[] asc = new byte[(int) ze.getSize()];
            count = 0;
            // Read the entire data for the file.
            while (count < ze.getSize()) {
                count += zis.read(asc, count, (int) (ze.getSize() - count));
            }
            process(readFile(asc));
            zis.closeEntry();
            zis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read and parse ASC grid data (in data).
     *
     * @param data ASC (ARC/INFO ASCII Grid) file contents.
     * @return ArrayList of every point in data, row-by-row.
     */
    private ArrayList<Float> readFile(byte[] data) {
        int pStart = 0;
        ArrayList<Float> points = new ArrayList<Float>();

        for (int pEnd = 0; pEnd < data.length; pEnd++) {
            if (data[pEnd] == '\n' || pEnd == (data.length - 1)) {
                String line = new String(data, pStart, pEnd - pStart);
                if (line.startsWith("ncols")) {
                    ncols = Integer.parseInt(line.substring(5).trim());
                } else if (line.startsWith("nrows")) {
                    nrows = Integer.parseInt(line.substring(5).trim());
                } else if (line.startsWith("xllcorner")) {
                    xll = (int) Float.parseFloat(line.substring(9).trim());
                } else if (line.startsWith("yllcorner")) {
                    yll = (int) Float.parseFloat(line.substring(9).trim());
                } else if (line.startsWith("cellsize")) {
                    cellsize = Float.parseFloat(line.substring(8).trim());
                } else if (line.startsWith("NODATA_value")) {
                    nodata = Float.parseFloat(line.substring(12).trim());
                } else {
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
        return points;
    }

    /**
     * Process the points read from the ASC file into vertices, normals and
     * edges for mesh, also generates texture coordinates.
     *
     * @param points The row-by-row points for the height map for which the mesh is
     *               to be created from.
     */
    private void process(ArrayList<Float> points) {
        downsample(3, points);
        vertices = new Vector3f[nrows * ncols];
        texcoord = BufferUtils.createFloatBuffer(2 * nrows * ncols);
        indexes = BufferUtils.createIntBuffer(6 * (nrows - 1) * (ncols - 1));
        Vector3f[][][] fnormals = new Vector3f[nrows - 1][ncols - 1][2];
        normals = new Vector3f[nrows * ncols];
        float x, z;

        System.out.println("Processing " + nrows + "x" + ncols + " terrain...");

        // First make vertices + texcoords
        x = 0.0f;
        for (int row = 0; row < nrows; row++) {
            z = 0.0f;
            for (int col = 0; col < ncols; col++) {
                vertices[(row * ncols) + col] = new Vector3f(x, data[row][col], z);
                texcoord.put(col / (float) ncols);
                texcoord.put(row / (float) nrows);
                z += cellsize;
            }
            x += cellsize;
        }
        // Indexes + face normals
        System.out.println("Populating faces...");
        for (int row = 0; row < (nrows - 1); row++) {
            for (int col = 0; col < (ncols - 1); col++) {
                int base = 6 * ((row * (ncols - 1)) + col);
                // Triangle 1
                indexes.put((row * ncols) + col); // a
                indexes.put((row * ncols) + col + 1); // b
                indexes.put(((row + 1) * ncols) + col); // c
                // Triangle 2
                indexes.put(((row + 1) * ncols) + col); // c
                indexes.put((row * ncols) + col + 1); // b
                indexes.put(((row + 1) * ncols) + col + 1); // d
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
                // ormal.subtractLocal(pointa).crossLocal(pointc.x - pointa.x,
                // pointc.y - pointa.y, pointc.z - pointa.z);
                fnormals[row][col][0] = ba.crossLocal(ca).normalizeLocal();
                fnormals[row][col][1] = bc.crossLocal(dc).normalizeLocal();
                // Triangle ta = new Triangle(a,b,c);
                // Triangle tb = new Triangle(c,b,d);
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
        t = fnormals[0][ncols - 2][0].add(fnormals[0][ncols - 2][1]);
        t.normalizeLocal();
        normals[ncols - 1] = t;
        // BL
        t = fnormals[nrows - 2][0][0].add(fnormals[nrows - 2][0][1]);
        t.normalizeLocal();
        normals[(nrows - 1) * ncols] = t;
        // BR
        t = fnormals[nrows - 2][ncols - 2][0].add(fnormals[nrows - 2][ncols - 2][1]);
        t.normalizeLocal();
        normals[(nrows * ncols) - 1] = t;
        // Do first row and last row
        for (int col = 1; col < (ncols - 1); col++) {
            // First row
            t = fnormals[0][col - 1][0].add(fnormals[0][col - 1][1]);
            t.addLocal(fnormals[0][col][0]);
            t.addLocal(fnormals[0][col][1]);
            t.normalizeLocal();
            normals[col] = t;
            // Last row
            t = fnormals[nrows - 2][col - 1][0].add(fnormals[nrows - 2][col - 1][1]);
            t.addLocal(fnormals[nrows - 2][col][0]);
            t.addLocal(fnormals[nrows - 2][col][1]);
            t.normalizeLocal();
            normals[((nrows - 1) * ncols) + col] = t;
        }
        // Do first and last column
        for (int row = 1; row < (nrows - 1); row++) {
            // First col
            t = fnormals[row - 1][0][0].add(fnormals[row - 1][0][1]);
            t.addLocal(fnormals[row][0][0]);
            t.addLocal(fnormals[row][0][1]);
            t.normalizeLocal();
            normals[row * ncols] = t;
            // Last col
            t = fnormals[row - 1][ncols - 2][0].add(fnormals[row - 1][ncols - 2][1]);
            t.addLocal(fnormals[row][ncols - 2][0]);
            t.addLocal(fnormals[row][ncols - 2][1]);
            t.normalizeLocal();
            normals[(row * ncols) + (ncols - 1)] = t;
            // Centre cols
            for (int col = 1; col < (ncols - 1); col++) {
                t = fnormals[row - 1][col - 1][0].add(fnormals[row - 1][col - 1][1]);
                t.addLocal(fnormals[row - 1][col][0]);
                t.addLocal(fnormals[row - 1][col][1]);
                t.addLocal(fnormals[row][col - 1][0]);
                t.addLocal(fnormals[row][col - 1][1]);
                t.addLocal(fnormals[row][col][0]);
                t.addLocal(fnormals[row][col][1]);
                t.normalizeLocal();
                normals[(row * ncols) + col] = t;
            }
        }
        System.out.println("Processing finished...");
        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        setBuffer(VertexBuffer.Type.TexCoord, 2, texcoord);
        setBuffer(VertexBuffer.Type.Index, 3, indexes);
        setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        updateBound();
        System.out.println("NE corner: " + vertices[vertices.length - 1].mult(0.006f));
        // Hint to renderer that we don't plan on modifying the terrain.
        this.setStatic();
    }

    /**
     * Downsample the point data using average over a neighbourhood.
     *
     * @param nsize  Size of the neighbourhood to use.
     * @param points Row-by-row point data.
     */
    private void downsample(int nsize, ArrayList<Float> points) {
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

    // Vertex normals
    public Vector3f[] getNormals() {
        return normals;
    }

    // Vertices
    public Vector3f[] getVertices() {
        return vertices;
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

    public int getXll() {
        return xll;
    }

    public int getYll() {
        return yll;
    }
}

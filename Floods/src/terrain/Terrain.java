package terrain;


import java.util.ArrayList;

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.material.Material;

import water.Particle;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

//http://environment.data.gov.uk/ds/survey/index.jsp
public class Terrain {
	ASCTerrain ater;
	Geometry g;
	static final float scale = 0.005f;
	
	public Terrain(Material mat) {
		ater = new ASCTerrain("lidar.zip");
		g = new Geometry("Terrain", ater);
        g.setMaterial(mat);
        g.scale(scale, scale, scale);
	}
	
	
	public ArrayList<Boolean> collide(Particle p, float t) {
		// Find closest row/col for particle
		// y axis bounding box.
		// Every triangle in box AND surrounding boxes
		// We assume that only scales in all 3 axes are done
		// Similarly no rotations, hence we can reuse our earlier normals.
		// However, we must translate a,b,c
		// Make p's position relative to terrain.
		Vector3f vertices[] = ater.getVertices();
		Vector3f prel = p.getWorldTranslation().subtract(g.getWorldTranslation());
		Vector3f fnormals[][][] = ater.getFaceNormals();
		ArrayList<Boolean> collisions = new ArrayList<Boolean>();
		// Is prel within the terrain (x,z) only for now?
		float cellsize = ater.getCellsize() * scale;
		int nrows = ater.getRows();
		int ncols = ater.getCols();
		
		int row, col;
		if (prel.x >= 0.0 && prel.x <= (cellsize * nrows) 
				&& prel.z >= 0.0 && prel.z <= (cellsize * ncols)) {
			row = (int) Math.floor(prel.x/cellsize);
			col = (int) Math.floor(prel.z/cellsize);
			int srow = (row == 0) ? 0 : row - 1;
			int erow = (row+2) > nrows - 1 ? nrows - 1 : row+2;
			int scol = (col == 0) ? 0 : col - 1;
			int ecol = (col+2) > ncols - 1 ? ncols - 1 : col+2;
			for (int r = srow; r < erow; r++) {
				for (int c = scol; c < ecol; c++) {
					int base = (r * ncols) + c;
					Vector3f v1 = vertices[base].mult(g.getWorldScale());
					v1.addLocal(g.getWorldTranslation());
					Vector3f v2 = vertices[base + 1].mult(g.getWorldScale());
					v2.addLocal(g.getWorldTranslation());
					Vector3f v3 = vertices[base + ncols].mult(g.getWorldScale());
					v3.addLocal(g.getWorldTranslation());
					Vector3f v4 = vertices[base + ncols + 1].mult(g.getWorldScale());
					v4.addLocal(g.getWorldTranslation());
					// Triangle 1
					if (collideTriangle(p, t, v1, v2, v3, fnormals[r][c][0])) {
						collisions.add(true);
					}
					// Triangle 2
					if (collideTriangle(p, t, v3, v2, v4, fnormals[r][c][0])) {
						collisions.add(true);
					}
				}
			}
		}
		else {
			// No col possible - or + cellsize?
			// System.out.println("NOPE");
		}
		return collisions;
	}
	
	// http://www.peroxide.dk/papers/collision/collision.pdf
	//https://hub.jmonkeyengine.org/t/ellipsoid-collision-detection-system/2064/2
	// Test collision with a single triangle
	private boolean collideTriangle(Particle p, float t,
			Vector3f a, Vector3f b, Vector3f c, Vector3f norm) {
		// scale triangle based on particle size.
		float invradius = 1/p.getRadius();
		Vector3f ta = a.mult(invradius);
		Vector3f tb = b.mult(invradius);
		Vector3f tc = c.mult(invradius);
		float planeeq = -(norm.x*ta.x + norm.y * ta.y + norm.z * ta.z);
		// Colliding against the back of the triangle
        if (norm.dot(p.getVelocity().normalize()) >= 0) {
        	return false;
        }
        // Else carry on....
        float distance = p.getWorldTranslation().dot(norm) + planeeq;
        float normdotvel = p.getVelocity().dot(norm);
        float t0, t1;
        boolean embedded = false;
        if (normdotvel == 0.0) {
        	// Sphere is parallel to plane.
        	if (Math.abs(distance) >= 1.0) {
        		// Sphere is not embedded, no collision.
        		return false;
        	} else {
        		// Sphere is embedded. Ooops.
        		embedded = true;
        		t0 = 0;
        		t1 = 1;
        	}	
        } else {
        	t0 = (-1.0f - distance)/normdotvel;
        	t1 = (1.0f - distance)/normdotvel;
        	// Ensure t0 > t1
        	if (t0  > t1) {
        		float temp = t1;
        		t1 = t0;
        		t0 = temp;
        	}
        	// Check if within range.
        	if (t0 > 1.0f || t1 < 0.0f) {
        		return false;
        	}
            // Clamp to [0,1]
            if (t0 < 0.0) t0 = 0.0f;
            if (t1 < 0.0) t1 = 0.0f;
            if (t0 > 1.0) t0 = 1.0f;
            if (t1 > 1.0) t1 = 1.0f;

        }
        if (t0 >= t) {
        	return false;
        }
        
        if (!embedded) {
        	// Calculate intersection point.
        	Vector3f intersect = p.getWorldTranslation().subtract(norm);
        	Vector3f v = p.getVelocity().mult(t0);
        	intersect.addLocal(v);
        	// Is this point in the triangle?
        	if (inTriangle(intersect, ta, tb, tc)) {
        		return true;
        	}
        }
        float velsqr = p.getVelocity().lengthSquared();
        // there's more, but we don't care about embedded particles yet.
		return true;
	}
	
	private boolean inTriangle(Vector3f p, Vector3f a, Vector3f b, Vector3f c) {
		Vector3f ta = a.subtract(p);
		Vector3f tb = b.subtract(p);
		Vector3f tc = c.subtract(p);
		
		ta.normalizeLocal();
		tb.normalizeLocal();
		tc.normalizeLocal();
		
		float angle = (float) (Math.acos(ta.dot(tb)) + Math.acos(tb.dot(tc)) + Math.acos(tc.dot(ta)));
		return Math.abs(angle - (FastMath.TWO_PI))  < 0.01;
	}
	
	public Geometry getGeometry() {
		return g;
	}
}

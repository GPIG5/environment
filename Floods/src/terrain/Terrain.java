package terrain;


import java.util.ArrayList;

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.material.Material;

import water.Particle;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Plane;
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
					// Extract vectors and scale to world size.
					Vector3f v1 = vertices[base].mult(g.getWorldScale());
					v1.addLocal(g.getWorldTranslation());
					Vector3f v2 = vertices[base + 1].mult(g.getWorldScale());
					v2.addLocal(g.getWorldTranslation());
					Vector3f v3 = vertices[base + ncols].mult(g.getWorldScale());
					v3.addLocal(g.getWorldTranslation());
					Vector3f v4 = vertices[base + ncols + 1].mult(g.getWorldScale());
					v4.addLocal(g.getWorldTranslation());
					//System.out.println("v1: " + v1 + " v2: " + v2 + " v3: " + v3 + " v4: " + v4);
					// Triangle 1
					// Test for collision.
					if (collideTriangle(p, t, v1, v2, v3, fnormals[r][c][0])) {
						collisions.add(true);
					}
					// Triangle 2
					if (collideTriangle(p, t, v3, v2, v4, fnormals[r][c][1])) {
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
	// Todo: wrap cpoint and t0, t1 into collision result object.
	// http://www.realtimerendering.com/intersections.html
	/**
	 * @param p
	 * The particle with which the terrain is colliding with.
	 * @param time
	 * The max time that will be considered for collisions.
	 * @param a
	 * First point of triangle in mesh.
	 * @param b
	 * Second point of triangle in mesh.
	 * @param c
	 * Third point of triangle in mesh.
	 * @param norm
	 * Normal of triangle.
	 * @return
	 */
	private boolean collideTriangle(Particle p, float time,
			Vector3f a, Vector3f b, Vector3f c, Vector3f norm) {
		// Inverse radius.
		float invrad = 1/p.getRadius();
		// Velocity of particle.
		Vector3f velocity = p.getVelocity();
		// Position of particle.
		Vector3f base =  p.getWorldTranslation();

		// scale triangle based on particle size.
		Vector3f evel = velocity.mult(invrad);
		Vector3f ea = a.mult(invrad);
		Vector3f eb = b.mult(invrad);
		Vector3f ec = c.mult(invrad);
		//System.out.println(a + " " + ea);
	
		if (norm.dot(evel.normalize()) >= 0) {
			// Not facing plane.
			return false;
		}
		
		float sdist = p.getWorldTranslation().dot(norm) - norm.dot(ea);
		float t0, t1;
		boolean embedded = false;
		float normDotVel = norm.dot(evel);
		
		// if sphere is traveling parallel to plane...
		if (normDotVel == 0.0f) {
			if (Math.abs(sdist) >= 1.0f) {
				// Sphere is not embedded in plane.
				return false;
			}
			else {
				// sphere is embedded in plane.
				// Intersects for range 0..1
				embedded = true;
				t0 = 0.0f;
				t1 = 1.0f;
			}
		}
		else {
			// Calculate intersection interval.
			t0 = (-1-sdist)/normDotVel;
			t1 = (1-sdist)/normDotVel;
			
			// Swap such t0 < t1
			if (t0 > t1) {
				float temp = t1;
				t1 = t0;
				t0 = temp;
			}
			
			// Check that within range
			if (t0 > 1.0f || t1 < 0.0f) {
				// Both t values are outside 0..1
				// Can not collide.
				return false;
			}
			
			// Clamp to 0..1
			if (t0 < 0.0) {
				t0 = 0.0f;
			}
			if (t1 < 0.0f) {
				t1 = 0.0f;
			}
			if (t0 > 1.0f) {
				t0 = 1.0f;
			}
			if (t1 > 1.0f) {
				t1 = 1.0f;
			}
			
		}
		// We now have t0 and t1 at which sphere
		// intersects the triangle's plane.
		// We now check for collisions in this interval.
		Vector3f cpoint;
		boolean cfound = false;
		float t = 1.0f;
		
		// Check for collision inside the triangle.
		// This must happen at t0.
		if (!embedded) {
			Vector3f intersect = 
					p.getWorldTranslation().subtract(norm);
			intersect.addLocal(velocity.mult(t0));
			if (inTriangle(base, ea, eb, ec)) {
				return true;
				// cfound = true;
				//t = t0;
				//cpoint = intersect;
			}
		}
		
		// Check against points.
		float velsqr = velocity.lengthSquared();
		float c2, c1, c0;
		c2 = velsqr;
		Root root = new Root();
		// point A
		c1 = 2.0f * velocity.dot(base.subtract(ea));
		c0 = ea.subtract(base).lengthSquared() - 1.0f;
		if (getLowestRoot(c2, c1, c0, t, root)) {
			t = root.getRoot();
			//cfound = true;
			//cpoint = a;
			return true;
		}
		
		c1 = 2.0f * velocity.dot(base.subtract(eb));
		c0 = eb.subtract(base).lengthSquared() - 1.0f;
		if (getLowestRoot(c2, c1, c0, t, root)) {
			t = root.getRoot();
			//cfound = true;
			//cpoint = b;
			return true;
		}
		
		c1 = 2.0f*velocity.dot(base.subtract(ec));
		c0 = ec.subtract(base).lengthSquared() - 1.0f;
		if (getLowestRoot(c2, c1, c0, t, root)) {
			t = root.getRoot();
			// cfound = true;
			// cpoint = b;
			return true;
		}
		
		// Check edges
		// a -> b
		Vector3f edge = eb.subtract(ea);
		Vector3f baseToVertex = ea.subtract(base);
		float edgeSqrLen = edge.lengthSquared();
		float edgeDotVelocity = edge.dot(velocity);
		float edgeDotBaseToVertex = edge.dot(baseToVertex);
		
		// Equation parameters
		c2 = edgeSqrLen * -velsqr + edgeDotVelocity*edgeDotVelocity;
		c1 = edgeSqrLen * (2*velocity.dot(baseToVertex)) - 
				2.0f * edgeDotVelocity*edgeDotBaseToVertex;
		c0 = edgeSqrLen*(1-baseToVertex.lengthSquared()) + 
				edgeDotBaseToVertex*edgeDotBaseToVertex;
		if (getLowestRoot(c2, c1, c0, t, root)) {
			float f = (edgeDotVelocity * root.getRoot() -edgeDotBaseToVertex)/edgeSqrLen;
			if (f >= 0.0f && f <= 1.0f) {
				t = root.getRoot();
				// cfound = true;
				//cpoint = a.add(edge.mult(f));
				return true;
			}
		}
		
		// b -> c
		edge = ec.subtract(eb);
		baseToVertex = eb.subtract(base);
		edgeSqrLen = edge.lengthSquared();
		edgeDotVelocity = edge.dot(velocity);
		edgeDotBaseToVertex = edge.dot(baseToVertex);
		c2 = edgeSqrLen * -velsqr + edgeDotVelocity*edgeDotVelocity;
		c1 = edgeSqrLen * (2*velocity.dot(baseToVertex)) - 
				2.0f * edgeDotVelocity*edgeDotBaseToVertex;
		c0 = edgeSqrLen*(1-baseToVertex.lengthSquared()) + 
				edgeDotBaseToVertex*edgeDotBaseToVertex;
		
		if (getLowestRoot(c2, c1, c0, t, root)) {
			float f = (edgeDotVelocity * root.getRoot() -edgeDotBaseToVertex)/edgeSqrLen;
			if (f >= 0.0f && f <= 1.0f) {
				t = root.getRoot();
				// cfound = true;
				//cpoint = b.add(edge.mult(f));
				return true;
			}
		}
		
		// c -> a
		edge = ea.subtract(ec);
		baseToVertex = ec.subtract(base);
		edgeSqrLen = edge.lengthSquared();
		edgeDotVelocity = edge.dot(velocity);
		edgeDotBaseToVertex = edge.dot(baseToVertex);
		c2 = edgeSqrLen * -velsqr + edgeDotVelocity*edgeDotVelocity;
		c1 = edgeSqrLen * (2*velocity.dot(baseToVertex)) - 
				2.0f * edgeDotVelocity*edgeDotBaseToVertex;
		c0 = edgeSqrLen*(1-baseToVertex.lengthSquared()) + 
				edgeDotBaseToVertex*edgeDotBaseToVertex;
		
		if (getLowestRoot(c2, c1, c0, t, root)) {
			float f = (edgeDotVelocity * root.getRoot() -edgeDotBaseToVertex)/edgeSqrLen;
			if (f >= 0.0f && f <= 1.0f) {
				t = root.getRoot();
				// cfound = true;
				//cpoint = c.add(edge.mult(f));
				return true;
			}
		}
		
		
		
		return false;
		/*
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
		return true; */
	}
	
	
	private boolean inTriangle(Vector3f p, Vector3f a, Vector3f b, Vector3f c) {
		Vector3f ta = a.subtract(p);
		Vector3f tb = b.subtract(p);
		Vector3f tc = c.subtract(p);
		
		ta.normalizeLocal();
		tb.normalizeLocal();
		tc.normalizeLocal();
		
		float angle = (float) (Math.acos(ta.dot(tb)) + Math.acos(tb.dot(tc)) + Math.acos(tc.dot(ta)));
		return Math.abs(angle - (FastMath.TWO_PI)) < 0.01;
	}
	
	boolean getLowestRoot(float a, float b, float c, float max, Root r) {
		float det = b*b + 4.0f*a*c;
		if (det < 0.0f) {
			return false;
		}
		
		float sqrtD = (float) Math.sqrt(det);
		float r1 = (-b - sqrtD) / (2*a);
		float r2 = (-b + sqrtD) / (2*a);
		
		if (r1 > r2) {
			float temp = r2;
			r2 = r1;
			r1 = temp;
		}
		
		if (r1 > 0 && r1 < max) {
			r.setRoot(r1);
			return true;
		}
		
		if (r2 > 0 && r2 < max) {
			r.setRoot(r2);
			return true;
		}
		return false;
	}
	
	public Geometry getGeometry() {
		return g;
	}
}

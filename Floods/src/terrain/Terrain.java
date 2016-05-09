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
	// Value for excluding almost parallel spheres.
	static final float epsilon = 0.00005f;
	
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
	//https://github.com/jongber/AndroidOpengl/blob/f290fe0a58485151047a9c7cedc8138f4556c546/app/src/main/java/glproj/jongber/androidopengl/utils/joml/Intersectionf.java
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
	private boolean collideTriangle(Particle p, float tmax,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f norm) {
		Vector3f base = p.getWorldTranslation();
		Vector3f vel = p.getVelocity();
		float rad = p.getRadius();
		Vector3f v10 = v1.subtract(v0);
		Vector3f v20 = v2.subtract(v0);
		// Plane of triangle.
        float a = v10.y * v20.z - v20.y * v10.z;
        float b = v10.z * v20.x - v20.z * v10.x;
        float c = v10.x * v20.y - v20.x * v10.y;
        float d = -(a * v0.x + b * v0.y + c * v0.z);
        float invLen = (float) (1.0 / Math.sqrt(a * a + b * b + c * c));
        float signedDist = (a * base.x + b * base.y + c * base.z + d) * invLen;
        float dot = (a * vel.x + b * vel.y + c * vel.z) * invLen;
        // Nearly parallel.
        if (dot < epsilon && dot > -epsilon) {
        	return false;
        }
        float pt0 = (rad - signedDist)/dot;
        // Not in this time frame
        if (pt0 > tmax) {
        	return false;
        }
        float pt1 = (-rad - signedDist) / dot;
        float p0X = base.x - rad * a * invLen + vel.x * pt0;
        float p0Y = base.y - rad * b * invLen + vel.y * pt0;
        float p0Z = base.z - rad * c * invLen + vel.z * pt0;
        if (inTriangle(new Vector3f(p0X, p0Y, p0Z), v0, v10, v20)) {
        	// pointAndTime.x = p0X;\
        	// pointAndTime.y = p0Y;
        	// pointAndTime.z = p0Z;
            // pointAndTime.w = pt0;
        	return true;
        }
        float t0 = tmax;
        float A = vel.lengthSquared();
        float rad2 = rad * rad;
        // test against v0
        Vector3f centerV0 = base.subtract(v0);
        float B0 = 2.0f * vel.dot(centerV0);
        float C0 = centerV0.lengthSquared() - rad2;
        float root0 = getLowestRoot(A, B0, C0, t0);
        if (!Float.isNaN(root0) && root0 < t0) {
//            pointAndTime.x = v0X;
//            pointAndTime.y = v0Y;
//            pointAndTime.z = v0Z;
//            pointAndTime.w = root0;
            t0 = root0;
            return true;
        }
        // test against v1
        Vector3f centerV1 = base.subtract(v1);
        float centerV1Len = centerV1.lengthSquared();
        float B1 = 2.0f * vel.dot(centerV1);
        float C1 = centerV1Len - rad2;
        float root1 = getLowestRoot(A, B1, C1, t0);
        if (!Float.isNaN(root1) && root1 < t0) {
//            pointAndTime.x = v1X;
//            pointAndTime.y = v1Y;
//            pointAndTime.z = v1Z;
//            pointAndTime.w = root1;
            t0 = root1;
            return true;
        }
        // test against v2
        Vector3f centerV2 = base.subtract(v2);
        float B2 = 2.0f * (vel.x * centerV2.x + vel.y * centerV2.y + vel.z * centerV2.z);
        float C2 = centerV2.lengthSquared()- rad2;
        float root2 = getLowestRoot(A, B2, C2, t0);
        if (!Float.isNaN(root2) && root2 < t0) {
//            pointAndTime.x = v2X;
//            pointAndTime.y = v2Y;
//            pointAndTime.z = v2Z;
//            pointAndTime.w = root2;
            t0 = root2;
            return true;
        }
        float velLen = vel.lengthSquared();
        // test against edge10
        float len10 = v10.lengthSquared();
        float baseTo0Len = centerV0.lengthSquared();
        float v10Vel = v10.dot(vel);
        float A10 = len10 * -velLen + v10Vel * v10Vel;
        float v10BaseTo0 = v10.x * -centerV0.x + v10.y * -centerV0.y + v10.z * -centerV0.z;
        float velBaseTo0 = vel.x * -centerV0.x + vel.y * -centerV0.y + vel.z * -centerV0.z;
        float B10 = len10 * 2 * velBaseTo0 - 2 * v10Vel * v10BaseTo0;
        float C10 = len10 * (rad2 - baseTo0Len) + v10BaseTo0 * v10BaseTo0;
        float root10 = getLowestRoot(A10, B10, C10, t0);
        if (!Float.isNaN(root10)) {
	        float f10 = (v10Vel * root10 - v10BaseTo0) / len10;
	        if (f10 >= 0.0f && f10 <= 1.0f && root10 < t0) {
//	            pointAndTime.x = v0X + f10 * v10X;
//	            pointAndTime.y = v0Y + f10 * v10Y;
//	            pointAndTime.z = v0Z + f10 * v10Z;
//	            pointAndTime.w = root10;
	            t0 = root10;
	            return true;
	        }
        }
        // test against edge20
        float len20 = v20.lengthSquared();
        float v20Vel = v20.dot(vel);
        float A20 = len20 * -velLen + v20Vel * v20Vel;
        float v20BaseTo0 = v20.x * -centerV0.x + v20.y * -centerV0.y + v20.z * -centerV0.z;
        float B20 = len20 * 2 * velBaseTo0 - 2 * v20Vel * v20BaseTo0;
        float C20 = len20 * (rad2 - baseTo0Len) + v20BaseTo0 * v20BaseTo0;
        float root20 = getLowestRoot(A20, B20, C20, t0);
        if (!Float.isNaN(root20)) {
	        float f20 = (v20Vel * root20 - v20BaseTo0) / len20;
	        if (f20 >= 0.0f && f20 <= 1.0f && root20 < pt1) {
//	            pointAndTime.x = v0X + f20 * v20X;
//	            pointAndTime.y = v0Y + f20 * v20Y;
//	            pointAndTime.z = v0Z + f20 * v20Z;
//	            pointAndTime.w = root20;
	            t0 = root20;
	            return true;
	        }
        }
        // test against edge21
        Vector3f v21 =  v2.subtract(v1);
        float len21 = v21.lengthSquared();
        float baseTo1Len = centerV1Len;
        float v21Vel = v21.dot(vel);
        float A21 = len21 * -velLen + v21Vel * v21Vel;
        float v21BaseTo1 = v21.x * -centerV1.x + v21.y * -centerV1.y + v21.z * -centerV1.z;
        float velBaseTo1 = vel.x * -centerV1.x + vel.y * -centerV1.y + vel.z * -centerV1.z;
        float B21 = len21 * 2 * velBaseTo1 - 2 * v21Vel * v21BaseTo1;
        float C21 = len21 * (rad2 - baseTo1Len) + v21BaseTo1 * v21BaseTo1;
        float root21 = getLowestRoot(A21, B21, C21, t0);
        if (!Float.isNaN(root21)) {
	        float f21 = (v21Vel * root21 - v21BaseTo1) / len21;
	        if (f21 >= 0.0f && f21 <= 1.0f && root21 < t0) {
	//            pointAndTime.x = v1X + f21 * v21X;
	//            pointAndTime.y = v1Y + f21 * v21Y;
	//            pointAndTime.z = v1Z + f21 * v21Z;
	//            pointAndTime.w = root21;
	            t0 = root21;
	            return true;
	        }
        }
        return false;
	}
	
	// Fast triangle checker - From joml.Intersectionf.java
	private boolean inTriangle(Vector3f p, Vector3f v0, Vector3f v1, Vector3f v2) {		
        Vector3f e10 = v1.subtract(v0);        
        Vector3f e20 = v2.subtract(v0);        
        float a = e10.dot(e10);
        float b = e10.dot(e20);
        float c = e20.dot(e20);
        float ac_bb = a * c - b * b;
        Vector3f vp = p.subtract(v0);
        float d = vp.dot(e10);
        float e = vp.dot(e20);
        float x = d * c - e * b;
        float y = e * a - d * b;
        float z = x + y - ac_bb;
        return ((Float.floatToRawIntBits(z) & ~(Float.floatToRawIntBits(x) | Float.floatToRawIntBits(y))) & 0x80000000) != 0;
	}
	
	private float getLowestRoot(float a, float b, float c, float max) {
		float det = b*b + 4.0f*a*c;
		if (det < 0.0f) {
			return Float.NaN;
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
			return r1;
		}
		
		if (r2 > 0 && r2 < max) {
			return r2;
		}
		return Float.NaN;
	}
	
	public Geometry getGeometry() {
		return g;
	}
}

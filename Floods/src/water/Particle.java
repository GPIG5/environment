package water;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;

import terrain.Cell;
import terrain.Terrain;

public class Particle extends Geometry {
	Vector3f velocity;
	Vector3f force;
	Cell cell;
	
	float radius;
	// The number of ticks when this was last rendered.
	long ticks_last;
	
	public Particle(Vector3f pos, Vector3f v, Material mat, float r) {
		super("Particle", new Sphere(4,4, r));
		this.setMaterial(mat);
		this.move(pos);
		velocity = v;
		radius = r;
		ticks_last = -1;
	}
	
	public long getTicksLast() {
		return ticks_last;
	}
	
	public void setTicksLast(long t) {
		ticks_last = t;
	}
	
	public void move(float t) {
		// Move by delta t.
		this.move(velocity.mult(t));
	}
	
	public float getRadius() {
		return radius;
	}
	
	public void setVelocity(Vector3f v) {
		velocity = v;
	}
	
	public Vector3f getVelocity() {
		return velocity;
	}
	
	public void updateCell(Terrain t) {
		cell = t.getCell(this.getWorldTranslation());
	}
	
	public Cell getCell() {
		return cell;
	}
	
	public boolean collide(Particle p, float t) {
		Vector3f pvel = p.getVelocity();
		float angle = velocity.angleBetween(pvel);
		if (angle == 0.0f) {
			// Calculate the distance between these two lines.
			
		}
		else if (angle == FastMath.PI) {
			// other particle is heading opposite direction.
			return false;
			// could already be embedded tho.
		}
		// Are the two particles on parallel vectors?
		return false;
	}
}

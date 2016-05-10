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
	Vector3f acc;
	// Some sort of velocity averaging thing between frames.
	Vector3f ev;
	Cell cell;
	
	float radius;
	// The number of ticks when this was last rendered.
	long ticks_last;
	
	// rest_density.......
	float density = 1000;
	float pressure;
	
	public Particle(Vector3f pos, Vector3f a, Material mat, float r) {
		super("Particle", new Sphere(4,4, r));
		this.setMaterial(mat);
		this.move(pos);
		velocity = new Vector3f();
		radius = r;
		ev = new Vector3f(0,0,0);
		acc = a;
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
	
	public Vector3f getEv() {
		return ev;
	}
	
	public void updateEv() {
		// ev is average of ev and new velocity.
		ev.addLocal(velocity).mult(0.5f);
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
	
	public void setDensity(float d) {
		density = d;
	}
	
	public float getDensity() {
		return density;
	}
	
	public void setPressure(float p) {
		pressure = p;
	}
	
	public float getPressure() {
		return pressure;
	}
	
	public Vector3f getAccel() {
		return acc;
	}
	
	public void setAccel(Vector3f a) {
		acc = a;
	}
}

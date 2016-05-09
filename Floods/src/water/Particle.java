package water;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;

public class Particle extends Geometry {
	Vector3f velocity;
	float radius;
	
	public Particle(Vector3f pos, Vector3f v, Material mat, float r) {
		super("Particle", new Sphere(4,4, r));
		this.setMaterial(mat);
		this.move(pos);
		velocity = v;
		radius = r;
	}
	
	public void move(float t) {
		// Move by delta t.
		this.move(velocity.mult(t));
	}
	
	public float getRadius() {
		return radius;
	}
	
	public Vector3f getVelocity() {
		return velocity;
	}
	
}

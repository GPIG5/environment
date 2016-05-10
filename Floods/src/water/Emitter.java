package water;

import java.util.Random;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Sphere;

public class Emitter {
	Vector3f loc;
	Vector3f acc;
	Vector3f bounds;
	Material mat;
	int num;
	
	// location is bottom left corner
	// direction is starting vector for emitted particles
	// number is number of particles per timestep
	// bounds is the size of emit box.
	public Emitter(Vector3f location, Vector3f acceleration, int number, Vector3f b, Material material) {
		loc = location;
		acc = acceleration;
		num = number;
		mat = material;
		bounds = b;
	}
	
	public Particle[] emit() {
		Random rn =  new Random();
		Particle[] particles = new Particle[num];
		for (int n = 0; n < num; n++) {
			Vector3f pos = new Vector3f(bounds.getX() * rn.nextFloat(), 
					bounds.getY() * rn.nextFloat(), bounds.getZ() * rn.nextFloat());
			pos.addLocal(loc);
			particles[n] = new Particle(pos, acc, mat, 0.02f);
			
		}
		return particles;
	}
}

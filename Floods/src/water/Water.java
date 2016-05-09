package water;

import java.util.ArrayList;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import terrain.Terrain;
///https://github.com/benma/pysph/blob/master/src/sph/sph.py
public class Water extends Node {
	float viscosity = 250;
	//
	float k = 1000;
	// Smoothing length
	float h = 2.0f;
	ArrayList<Emitter> emitters;
	long ticks_last;
	ArrayList<Particle> grid[][];
	Terrain terrain;
	
	
	public Water(Terrain t) {
		super.setName("Water Body");
		emitters =  new ArrayList<Emitter>();
		ticks_last = System.currentTimeMillis();
		terrain = t;
	}
	
	public void addEmitter(Emitter e) {
		this.emitters.add(e);
	}
	
	private void emitParticles() {
		for (Emitter e : emitters) {
			Particle[] particles = e.emit();
			for (Particle p : particles) {
				this.attachChild(p);
			}
		}
	}
	
	//Monaghan's popular cubic spline kernel
	
	public void process() {
		emitParticles();
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = ticks_now - ticks_last;
		for (Spatial s : this.children) {
			if (s instanceof Particle) {
				Particle p = (Particle)s;
				if (p.getWorldTranslation().getY() < -2) {
					// Cull first
					p.removeFromParent();
				}
				else {
					if (terrain.collide(p, t).size() == 0) {
						p.move(t);
					}
					//if (p.collideWith(ter, cr) > 0) {
				
					//}
					//else {
						
					//}
				}
			}
		}
		ticks_last = ticks_now;
	}
}

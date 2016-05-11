package water;

import java.util.ArrayList;
import java.util.PriorityQueue;

import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import terrain.Cell;
import terrain.Neighbourhood;
import terrain.Terrain;

// Heavily based on: https://github.com/finallyjustice/sphfluid/blob/master/SPH_CPU_3D_v1/sph_system.cpp
// Also an absolute mess.
public class Water extends Node {
	final float viscosity = 3.5f;
	// rest density
	final float rest_density = 1000;
	// gas constant
	final float k = 3f;
	// Smoothing length/kernel
	final float h = 0.1f;
	final float h2 = h*h;
	
	Vector3f gravity = new Vector3f(0,-9.8f,0);
	
	// Mass of each particle
	final float mass = 0.02f;
	
	final float poly6_value = (float) (315.0f/(64.0f * FastMath.PI * Math.pow(h, 9)));;
	final float spiky_value = (float) (-45.0f/(FastMath.PI * Math.pow(h, 6)));
	final float visco_value = (float) (45.0f/(FastMath.PI * Math.pow(h, 6)));

	final float grad_poly6 = (float) (-945/(32 * FastMath.PI * Math.pow(h, 9)));
	final float lplc_poly6 = (float) (-945/(8 * FastMath.PI * Math.pow(h, 9)));
	final float self_dens = (float) (mass*poly6_value*Math.pow(h, 6));
	
	
	ArrayList<Emitter> emitters;
	long ticks_last;
	// Group particles by the row they occupy.
	ArrayList<Particle> rows[];
	Terrain terrain;
	int nrows;
	int ncols;
	
	// https://wiki.manchester.ac.uk/sphysics/images/SPHysics_v2.2.000_GUIDE.pdf
	public Water(Terrain t) {
		super.setName("Water Body");
		// Params
		System.out.println("Poly 6 value" + poly6_value);
		// Misc
		emitters =  new ArrayList<Emitter>();
		ticks_last = System.currentTimeMillis();
		terrain = t;
		nrows =  terrain.getRows();
		ncols =  terrain.getCols();
		rows = new ArrayList[nrows];
		// Populate array lists.
		for (int r = 0; r < nrows; r++) {
			rows[r] = new ArrayList<Particle>();
		}
	}
	
	public void addEmitter(Emitter e) {
		this.emitters.add(e);
	}
	
	private void emitParticles() {
		for (Emitter e : emitters) {
			Particle[] particles = e.emit();
			for (Particle p : particles) {
				p.updateCell(terrain);
				Cell c = p.getCell();
				if (c.isValid()) {
					rows[c.getRow()].add(p);
					attachChild(p);
				}
			}
		}
	}
	
	/*
	private ArrayList<Boolean> collide(Particle p1, float t, Neighbourhood n) {
		// Scan through particle neighbourhood.
		for (int r = n.getSRow(); r <= n.getERow(); r++) {
			for (int c = n.getSCol(); c <= n.getECol(); c++) {
				for (Particle p2 : rows[r]) {
					
				}
			}
		}
		return null;
	} */
	
	
	// http://cg.informatik.uni-freiburg.de/intern/seminar/gridFluids_fluid-EulerParticle.pdf
	// https://wiki.manchester.ac.uk/sphysics/images/SPHysics_v2.2.000_GUIDE.pdf
	// https://www.cfa.harvard.edu/~pmocz/manuscripts/pmocz_sph.pdf
	// https://github.com/cbeach/sph/blob/master/particle/sph.cpp
	// https://nccastaff.bournemouth.ac.uk/jmacey/MastersProjects/MSc2010/08ChrisPriscott/ChrisPriscott_THESIS.pdf
	// Calculate the density and pressure of every particle.
	public void calcDP() {
		Particle pi;
		float density;
		// iterate over the children instead of rows.
		for (Spatial s : this.children) {
			if (s instanceof Particle) {
				pi = (Particle)s;
				density = self_dens;
				Cell c = pi.getCell();
				Vector3f pos = pi.getWorldTranslation();
				// Consider 3 consecutive rows.
				int crow = c.getRow();
				int srow = crow <= 0 ? 0 : crow-1;
				int erow = crow >= (nrows - 1) ? nrows - 1 : crow + 1;   
				for (int r = srow; r <= erow; r++) {
					for (Particle pj : rows[r]) {
						if (!pi.equals(pj)) {
							Vector3f rel = pj.getWorldTranslation().subtract(pos);
							float r2 = rel.dot(rel);
							if (r2 < h2 && Float.isFinite(r2)) {
								density += mass * poly6_value * Math.pow(h2-r2, 3);
							}
						}
					}
				}
				pi.setDensity(density);
				pi.setPressure((float) (Math.pow(density / rest_density, 7) - 1) * k);
			}
		}
	}
	
	// https://github.com/watch3602004/SPH_iga/blob/master/SPH.java
	// Calculate viscosity, pressure, tension and gravity forces
	public void calcForce() {
		Particle pi;
		Vector3f acc;
		Vector3f rel_vel;
		for (Spatial s: this.children) {
			if (s instanceof Particle) {
				pi = (Particle)s;
				acc = new Vector3f();
				Cell c = pi.getCell();
				Vector3f pos = pi.getWorldTranslation();
				int crow = c.getRow();
				int srow = crow <= 0 ? 0 : crow-1;
				int erow = crow >= (nrows - 1) ? nrows - 1 : crow + 1;   
				for (int r = srow; r <= erow; r++) {
					for (Particle pj : rows[r]) {
						if (!pi.equals(pj)) {
							Vector3f rel = pos.subtract(pj.getWorldTranslation());
							float r2 = rel.lengthSquared();
							if (r2 < h2 && Float.isFinite(r2)) {
								float rij = (float) Math.sqrt(r2);
								float h_r = h - rij;
								
								float v = mass/pj.getDensity()/2;
								float pres_kernel = spiky_value * h_r * h_r;
								float t_force = v * (pi.getPressure()+pj.getPressure()) * pres_kernel;
								
								acc.subtractLocal(rel.mult(t_force/rij));
								
								rel_vel = pj.getEv().subtract(pi.getEv());
								
								float visc_kernel = visco_value*(h-r);
								t_force = v * viscosity * visc_kernel;
								acc.addLocal(rel_vel.mult(t_force));
							}
						}
					}
				}
				pi.setAccel(acc);
			}
		}
	}
	
	public void process() {
		emitParticles();
		ArrayList<Particle> lst;
		Particle p;
		PriorityQueue<CollisionResult> collisions;
		Vector3f vel;
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = (ticks_now - ticks_last)/1000.0f;
		ticks_last = ticks_now;
		calcDP();
		calcForce();
		// Final step.
		for (int r = 0; r < nrows; r++) {
			lst = rows[r];
			// Each particle in row.
			for (int i = 0; i < lst.size(); i++) {
				p = lst.get(i);
				// only render if last render time was in the past.
				if (p.getTicksLast() >= ticks_now) {
					continue;
				}
				//System.out.println("Accel:" + p.getAccel());
				//System.out.println("Density: " + p.getDensity());
				vel = p.getAccel().mult(t/p.getDensity()).add(gravity.mult(t));
				p.setVelocity(vel);
				//System.out.println("Velocity " + vel);
				
				Cell c = p.getCell();
				/*
				int crow = c.getRow();
				int srow = crow <= 0 ? 0 : crow-1;
				int erow = crow >= (nrows - 1) ? nrows - 1 : crow + 1;   
				// Do particle particle collisions.
				for (int rn = srow; rn <= erow; rn++) {
					for (Particle p1 : rows[rn]) {
					}
				} */						
				/*
				collisions = terrain.collide(p, t, terrain.getNeighbourhood(c, 3));
				if (collisions.size() == 0) {
					//System.out.println("No collisions possible");
					p.move(t);
				}
				else {
					//System.out.println("Moving till collide.");
					//System.out.println("Collide at: " + collisions.peek().getTime());
					// For now, just move to the point of impact.
					//p.move(min);
					p.getVelocity().setY(0);
					p.move(t);
				} */
				// Is the cell still on the grid?
				p.move(t);
				if (p.getWorldTranslation().y < 2) {
					Vector3f temp = new Vector3f(0,2-p.getLocalTranslation().y,0);
					p.move(temp);
					p.setVelocity(vel.mult(0.5f));
				}
				if (p.getWorldTranslation().x < 10) {
					Vector3f temp = new Vector3f(10-p.getLocalTranslation().x,0,0);
					p.move(temp);
					p.setVelocity(vel.mult(0.5f));
				}
				if (p.getWorldTranslation().x > 22) {
					Vector3f temp = new Vector3f(p.getLocalTranslation().x-22,0,0);
					p.move(temp);
					p.setVelocity(vel.mult(0.5f));
				}
				if (p.getWorldTranslation().z < 25) {
					Vector3f temp = new Vector3f(0,0,25-p.getLocalTranslation().z);
					p.move(temp);
					p.setVelocity(vel.mult(0.5f));
				}
				if (p.getWorldTranslation().z > 34) {
					Vector3f temp = new Vector3f(0,0,p.getLocalTranslation().z-34);
					p.move(temp);
					p.setVelocity(vel.mult(0.5f));
				}
				
				
				p.updateCell(terrain);
				c = p.getCell();
				p.setTicksLast(ticks_now);
				p.updateEv();
				if (c.isValid() && p.getWorldTranslation().y > -2) {
					if (r != c.getRow()) {
						// Row change.
						lst.remove(i);
						i--;
						rows[c.getRow()].add(p);
					}
				}
				else {
					lst.remove(i);
					i--;
					p.removeFromParent();
				}
			}
		}
	}
}

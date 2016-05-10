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

///https://github.com/benma/pysph/blob/master/src/sph/sph.py
public class Water extends Node {
	float viscosity = 250;
	// rest density
	float rest_density = 1000;
	// gas constant
	float k = 1.0f;
	// Smoothing length
	float h = 4.0f;
	
	float alphad;
	
	
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
		alphad = (float) (1/(Math.pow(FastMath.PI, 1.5) * h * h * h));
		// Misc
		emitters =  new ArrayList<Emitter>();
		ticks_last = System.currentTimeMillis();
		terrain = t;
		nrows =  terrain.getRows();
		ncols =  terrain.getCols();
		rows = new ArrayList[nrows];
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
	
	private float gaussianKernel(float r) {
		// q = r/h where r is the distance
		// Once particles are greater than distance 2h away, they will no longer affect the particles in question.
		return (float) (alphad * Math.exp((r*r)/(h*h)));
	}
	
	// http://cg.informatik.uni-freiburg.de/intern/seminar/gridFluids_fluid-EulerParticle.pdf
	// https://wiki.manchester.ac.uk/sphysics/images/SPHysics_v2.2.000_GUIDE.pdf
	// https://www.cfa.harvard.edu/~pmocz/manuscripts/pmocz_sph.pdf
	// https://github.com/cbeach/sph/blob/master/particle/sph.cpp
	// https://github.com/finallyjustice/sphfluid/tree/master/SPH_CPU_3D_v1
	// https://github.com/finallyjustice/sphfluid/blob/master/SPH_CPU_3D_v1/sph_system.cpp
	// https://nccastaff.bournemouth.ac.uk/jmacey/MastersProjects/MSc2010/08ChrisPriscott/ChrisPriscott_THESIS.pdf
	// Calculate the density and pressure of every particle.
	public void calcDP() {
		Particle pi;
		float density;
		// iterate over the children instead of rows.
		for (Spatial s : this.children) {
			density = 0.0f;
			if (s instanceof Particle) {
				pi = (Particle)s;
				density = 0.0f;
				Cell c = pi.getCell();
				Vector3f pos = pi.getWorldTranslation();
				// Consider 3 consecutive rows.
				int crow = c.getRow();
				int srow = crow <= 0 ? 0 : crow-1;
				int erow = crow >= (nrows - 1) ? nrows - 1 : crow + 1;   
				for (int r = srow; r <= erow; r++) {
					for (Particle pj : rows[r]) {
						float rij = pos.distance(pj.getWorldTranslation());	
						float w = gaussianKernel(rij);
						density += w * pj.getMass();
					}
				}
				pi.setDensity(density);
				pi.setPressure(k*(density - rest_density));	
			}
		}
	}
	
	// Calculate viscosity, pressure, tension and gravity forces
	public void calcVPTG() {
		
	}
	
	public void process() {
		emitParticles();
		ArrayList<Particle> lst;
		Particle p;
		PriorityQueue<CollisionResult> collisions;
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = ticks_now - ticks_last;
		ticks_last = ticks_now;
		calcDP();
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
				Cell c = p.getCell();
				int crow = c.getRow();
				int srow = crow <= 0 ? 0 : crow-1;
				int erow = crow >= (nrows - 1) ? nrows - 1 : crow + 1;   
				// Calculate density.
				// Calculate pressure.
				for (int rn = srow; rn <= erow; rn++) {
					for (Particle p1 : rows[rn]) {
					}
				}						
				// Do world collisions.
				// Do particle collisions.
				// Move cell.
				collisions = terrain.collide(p, t, terrain.getNeighbourhood(c, 3));
				if (collisions.size() == 0) {
					p.move(t);
				}
				else {
					// For now, just move to the point of impact.
					p.setVelocity(new Vector3f());
					p.move(collisions.peek().getTime());
				}
				// Is the cell still on the grid?
				p.updateCell(terrain);
				c = p.getCell();
				p.setTicksLast(ticks_now);
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

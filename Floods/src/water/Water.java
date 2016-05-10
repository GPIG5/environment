package water;

import java.util.ArrayList;

import com.jme3.collision.CollisionResults;
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
	//
	float k = 1000;
	// Smoothing length
	float h = 2.0f;
	ArrayList<Emitter> emitters;
	long ticks_last;
	// Every particle is contained within a grid square, for pruning.
	ArrayList<Particle> grid[][];
	Terrain terrain;
	int nrows;
	int ncols;
	
	
	public Water(Terrain t) {
		super.setName("Water Body");
		emitters =  new ArrayList<Emitter>();
		ticks_last = System.currentTimeMillis();
		terrain = t;
		nrows =  terrain.getRows();
		ncols =  terrain.getCols();
		grid = new ArrayList[nrows][ncols];
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				grid[r][c] = new ArrayList<Particle>();
			}
		}
	}
	
	public void addEmitter(Emitter e) {
		this.emitters.add(e);
	}
	
	private void emitParticles() {
		for (Emitter e : emitters) {
			Particle[] particles = e.emit();
			for (Particle p : particles) {
				// Add to correct grid cell.
				Cell c = terrain.getCell(p.getWorldTranslation());
				if (c.isValid()) {
					this.attachChild(p);
				}
			}
		}
	}
	
	private ArrayList<Boolean> collide(Particle p1, float t, Neighbourhood n) {
		// Scan through particle neighbourhood.
		for (int r = n.getSRow(); r <= n.getERow(); r++) {
			for (int c = n.getSCol(); c <= n.getECol(); c++) {
				for (Particle p2 : grid[r][c]) {
					
				}
			}
		}
		return null;
	}
	
	public void process() {
		emitParticles();
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = ticks_now - ticks_last;
		for (Spatial s : this.children) {
			if (s instanceof Particle) {
				Particle p = (Particle)s;
				Cell c = terrain.getCell(p.getWorldTranslation());
				if (!c.isValid() || p.getWorldTranslation().getY() < -2) {
					// Cull first
					p.removeFromParent();
				}
				else {
					Neighbourhood n = terrain.getNeighbourhood(c, 3);
					if (terrain.collide(p, t, n).size() == 0) {
						p.move(t);
					}
					
					// Has the particle moved cell.
				}
			}
		}
		ticks_last = ticks_now;
	}
}

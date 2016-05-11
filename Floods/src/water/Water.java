package water;

import java.util.ArrayList;
import java.util.PriorityQueue;

import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import terrain.Terrain;

// Heavily based on: https://github.com/finallyjustice/sphfluid/blob/master/SPH_CPU_3D_v1/sph_system.cpp
// Also an absolute mess.
public class Water extends Node {
	long ticks_last;
	// Group particles by the row they occupy.
	Cell[][] grid;
	Terrain terrain;
	int nrows;
	int ncols;
	
	// https://wiki.manchester.ac.uk/sphysics/images/SPHysics_v2.2.000_GUIDE.pdf
	public Water(Terrain t, Material mat) {
		super.setName("Water Body");
		// Params
		ticks_last = System.currentTimeMillis();
		terrain = t;
		grid =  terrain.makeCells();
		nrows = grid.length;
		ncols = grid[0].length;
		System.out.println("Adding water cells to scene...");
		for (int r = 0; r < nrows-1; r++) {
			for (int c = 0; c < ncols-1; c++) {
				grid[r][c].setMaterial(mat);
				attachChild(grid[r][c]);
			}
		}
		grid[0][0].setVoume(2);
	}
	
	public void process() {
		Vector3f vel;
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = (ticks_now - ticks_last)/1000.0f;
		ticks_last = ticks_now;
		for (int r = 0; r < nrows-1; r++) {
			for (int c = 0; c < ncols-1; c++) {
				grid[r][c].process(t);
			}
		}
		for (int r = 0; r < nrows-1; r++) {
			for (int c = 0; c < ncols-1; c++) {
				grid[r][c].redraw(t);
			}
		}
	}
}

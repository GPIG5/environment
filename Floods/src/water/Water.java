package water;

import java.util.ArrayList;
import java.util.PriorityQueue;

import com.jme3.collision.CollisionResults;
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
	public Water(Terrain t) {
		super.setName("Water Body");
		// Params
		ticks_last = System.currentTimeMillis();
		terrain = t;
		nrows =  terrain.getRows();
		ncols =  terrain.getCols();
		grid =  terrain.makeCells();
	}
	
	// Add initial columns.
	public void addWater(int r, int c, float h) {
		
	}
	
	public void process() {
		Vector3f vel;
		long ticks_now = System.currentTimeMillis();
		// How much time this frame represents.
		float t = (ticks_now - ticks_last)/1000.0f;
		ticks_last = ticks_now;
	}
}

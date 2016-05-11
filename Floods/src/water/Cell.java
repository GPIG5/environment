package water;

import com.jme3.math.Vector3f;

// Hydrostatic pipe model.
// Dynamic Simulation of Splashing Fluids - O'Brien and Hodgins
// Interactive Terrain Modeling Using Hydraulic Erosion - Stava, Benes, Brisbin

public class Cell {
	// Each cell has pipes diagonally vertically and horizontally.
	private Cell[] neighbours;
	
	
	public Cell(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
		// TODO Auto-generated constructor stub
		neighbours = new Cell[8];
	}
}

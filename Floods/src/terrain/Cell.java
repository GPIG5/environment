package terrain;

public class Cell {
	int row;
	int col;
	
	
	public Cell(int r, int c) {
		row = r;
		col = c;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getCol() {
		return col;
	}
	
	public boolean isValid() {
		return row != -1 && col != -1;
	}
	
	public boolean equals(Cell c) {
		return row == c.getRow() && col == c.getCol();
	}
}

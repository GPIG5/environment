package terrain;

public class Neighbourhood {
	private boolean ingrid = true;
	private int srow;
	private int scol;
	private int row;
	private int col;
	private int erow;
	private int ecol;
	
	public Neighbourhood(int r, int c, int sr, int sc, int er, int ec, boolean i) {
		srow = sr;
		scol = sc;
		row = r;
		col = c;
		erow = er;
		ecol = ec;
		ingrid = i;
	}
	
	public int getSRow() {
		return srow;
	}
	
	public int getSCol() {
		return scol;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getCol() {
		return col;
	}
	
	public int getERow() {
		return erow;
	}
	
	public int getECol() {
		return ecol;
	}
	
	public boolean isInGrid() {
		return ingrid;
	}
	
	
}

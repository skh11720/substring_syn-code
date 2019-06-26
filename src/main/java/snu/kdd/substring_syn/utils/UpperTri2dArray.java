package snu.kdd.substring_syn.utils;

public class UpperTri2dArray {
	final int n;
	final int size;
	final int[] cell;
	
	public UpperTri2dArray( int n ) {
		this.n = n;
		size = n*(n+1)/2;
		cell = new int[size];
	}
	
	public int get( int i, int j ) {
		return cell[getPos(i, j)];
	}
	
	public void set( int i, int j, int val ) {
		cell[getPos(i, j)] = val;
	}

	private int getPos( int i, int j ) {
		// i, j are 0-based
		return size - (n-i)*(n-i+1)/2 - i + j;
	}
}

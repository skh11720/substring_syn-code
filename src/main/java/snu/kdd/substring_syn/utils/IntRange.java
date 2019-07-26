package snu.kdd.substring_syn.utils;

public class IntRange {
	public int min;
	public int max;
	
	public IntRange( int min, int max ) {
		this.min = min;
		this.max = max;
	}
	
	@Override
	public String toString() {
		return String.format("[%d, %d]", min, max);
	}
}

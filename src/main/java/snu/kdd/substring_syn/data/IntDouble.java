package snu.kdd.substring_syn.data;

import java.util.Comparator;

public class IntDouble {
	public int k;
	public double v;
	
	public static IntDoubleComp comp = new IntDoubleComp();
	
	public IntDouble( int k, double v ) {
		this.k = k;
		this.v = v;
	}
	
	@Override
	public String toString() {
		return String.format("(%d, %.3f)", k, v);
	}
	
	public static class IntDoubleComp implements Comparator<IntDouble> {

		@Override
		public int compare(IntDouble o1, IntDouble o2) {
			return -Double.compare(o1.v, o2.v);
		}
	}
}

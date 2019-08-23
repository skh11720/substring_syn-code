package snu.kdd.substring_syn.data;

import java.util.Comparator;

public class IntLong {
	public int k;
	public long v;
	
	public static IntLongComp comp = new IntLongComp();
	
	public IntLong( int k, long v ) {
		this.k = k;
		this.v = v;
	}
	
	@Override
	public String toString() {
		return String.format("(%d, %.3f)", k, v);
	}
	
	public static class IntLongComp implements Comparator<IntLong> {

		@Override
		public int compare(IntLong o1, IntLong o2) {
			return -Long.compare(o1.v, o2.v);
		}
	}
}

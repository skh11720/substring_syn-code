package snu.kdd.substring_syn.data;

import java.util.Comparator;

import snu.kdd.substring_syn.utils.Util;

public class IntPair implements Comparable<IntPair> {
	
	public int i1, i2;
	
	public IntPair() {
	}

	public IntPair( int i1, int i2 ) {
		this.i1 = i1;
		this.i2 = i2;
	}

	private int getHash() {
		// djb2-like
		int hash = Util.bigprime;
		hash = ( hash << 5 ) + Util.bigprime + i1;
		hash = ( hash << 5 ) + Util.bigprime + i2;
		return hash;
	}
	
	@Override
	public int hashCode() {
		return getHash();
	}
	
	@Override
	public String toString() {
		return String.format("(%d, %d)", i1, i2);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		IntPair o = (IntPair)obj;
		return (i1 == o.i1 && i2 == o.i2);
	}

	@Override
	public int compareTo(IntPair o) {
		if ( i1 < o.i1 ) return -1;
		else if ( i1 > o.i1 ) return 1;
		else {
			if ( i2 < o.i2 ) return -1;
			else if ( i2 > o.i2 ) return 1;
			else return 0;
		}
	}
	
	public static Comparator<IntPair> keyComparator() {
		return new Comparator<IntPair>() {

			@Override
			public int compare(IntPair o1, IntPair o2) {
				return Integer.compare(o1.i1, o2.i1);
			}
		};
	}
}

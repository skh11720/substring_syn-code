package snu.kdd.substring_syn.data;

public class IntPair implements Comparable<IntPair> {
	
	public final int i1, i2;
	private final int hash;

	public IntPair( int i1, int i2 ) {
		this.i1 = i1;
		this.i2 = i2;
		this.hash = getHash();
	}

	private int getHash() {
		int hash = 0;
		hash = ( hash << 32 ) + i1;
		hash = ( hash << 32 ) + i2;
		return hash;
	}
	
	@Override
	public int hashCode() {
		return hash;
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
}

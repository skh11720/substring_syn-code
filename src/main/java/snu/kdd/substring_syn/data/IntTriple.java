package snu.kdd.substring_syn.data;

import snu.kdd.substring_syn.utils.Util;

public class IntTriple {

	public final int i1, i2, i3;
	private final int hash;

	public IntTriple( int i1, int i2, int i3 ) {
		this.i1 = i1;
		this.i2 = i2;
		this.i3 = i3;
		this.hash = getHash();
	}

	private int getHash() {
		// djb2-like
		int hash = Util.bigprime;
		hash = ( hash << 5 ) + Util.bigprime + i1;
		hash = ( hash << 5 ) + Util.bigprime + i2;
		hash = ( hash << 5 ) + Util.bigprime + i3;
		return hash;
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public String toString() {
		return String.format("(%d, %d, %d)", i1, i2, i3);
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		IntTriple o = (IntTriple)obj;
		return (i1 == o.i1 && i2 == o.i2 && i3 == o.i3);
	}
}

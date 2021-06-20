package snu.kdd.pkwise;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Util;

public class KwiseSignature {

	private final int[] values;
	private final int hash;
	
	public KwiseSignature( int[] values ) {
		this.values = Arrays.copyOf(values, values.length);
		this.hash = setHash();
	}
	
	public IntIterable getValues() {
		return IntArrayList.wrap(values);
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		KwiseSignature o = (KwiseSignature)obj;
		if ( this.values.length != o.values.length ) return false;
		else {
			for ( int i=0; i<values.length; ++i ) {
				if ( this.values[i] != o.values[i] ) return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(values);
	}

	public String toOriginalString() {
		StringBuilder bld = new StringBuilder("["+Record.tokenIndex.getToken(values[0]));
		for ( int i=1; i<values.length; ++i ) bld.append(","+Record.tokenIndex.getToken(values[i]));
		bld.append("]");
		return bld.toString();
	}

	private int setHash() {
		int hash = Util.bigprime;
		for( int val : values ) {
			hash = ( hash << 5 ) + Util.bigprime + val;
		}
		return (int) ( hash % Integer.MAX_VALUE );
	}
}

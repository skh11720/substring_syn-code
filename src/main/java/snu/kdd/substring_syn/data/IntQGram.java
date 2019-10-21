package snu.kdd.substring_syn.data;

import java.io.Serializable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Util;

public class IntQGram implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final int arr[];
	private final int hash;
	
	public IntQGram( int id, QGram qgram ) {
		arr = new int[qgram.tokens.length+1];
		arr[0] = id;
		for ( int i=1; i<=qgram.tokens.length; ++i ) arr[i] = qgram.tokens[i-1];
		hash = getHash();
	}
	
	public IntQGram( int arr[] ) {
		this.arr = arr;
		hash = getHash();
	}
	
	public final int size() {
		return arr.length-1; 
	}
	
	public Record toRecord() {
		return new Record(arr[0], IntArrayList.wrap(arr).subList(1, arr.length).toIntArray());
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		IntQGram o = (IntQGram)obj;
		if ( this.arr.length != o.arr.length ) return false;
		else {
			for ( int i=0; i<arr.length; ++i ) {
				if ( this.arr[i] != o.arr[i] ) return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("[%d, %s]", arr[0], IntArrayList.wrap(arr).subList(1, arr.length));
	}

	private int getHash() {
		// djb2-like
		int hash = 0;
		for( int val : arr ) {
			hash = ( hash << 5 ) + Util.bigprime + val;
		}
		return (int) ( hash % Integer.MAX_VALUE );
	}
}

package snu.kdd.substring_syn.data;

import java.util.Arrays;

import snu.kdd.substring_syn.utils.Util;

public class QGram {
	public final int[] tokens;
	private int hash;
	
	public QGram( int[] tokens ) {
		this.tokens = Arrays.copyOf(tokens, tokens.length);
		hash = getHash();
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		QGram o = (QGram)obj;
		if ( this.tokens.length != o.tokens.length ) return false;
		else {
			for ( int i=0; i<tokens.length; ++i ) {
				if ( this.tokens[i] != o.tokens[i] ) return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(tokens);
	}

	private int getHash() {
		int hash = 0;
		for( int token : tokens ) {
			hash = ( hash << 5 ) + Util.bigprime + token;
		}
		return (int) ( hash % Integer.MAX_VALUE );
	}
}

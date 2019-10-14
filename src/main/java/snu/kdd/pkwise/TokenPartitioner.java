package snu.kdd.pkwise;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrays;
import snu.kdd.substring_syn.data.record.Record;

public class TokenPartitioner {
	/*
	 * equi-width partitioning
	 */

	private final int[] partRange; // interval of class i is [partRange[i-1], partRange[i])

	public TokenPartitioner( int kmax ) {
		partRange = new int[kmax];
		int maxTokenId = Record.tokenIndex.getMaxID();
		for ( int i=0; i<kmax; ++i ) partRange[i] = (int)Math.ceil(maxTokenId*(1.0*(i+1)/kmax)+1e-5);
	}
	
	public int getTokenClass( int token ) {
		int pos = IntArrays.binarySearch(partRange, token);
		if ( pos < 0 ) pos = -pos - 1;
		return pos;
	}
	
	@Override
	public String toString() {
		return String.format("TokenPartitioner(%s)", Arrays.toString(partRange));
	}
}

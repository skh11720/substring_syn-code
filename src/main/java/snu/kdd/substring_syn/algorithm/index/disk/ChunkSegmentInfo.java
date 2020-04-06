package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ChunkSegmentInfo {
	final int fileOffset;
	final long offset;
	final IntArrayList chunkLenList;
	final int numInts;
	
	public ChunkSegmentInfo( int fileOffset, long offset, int numInts ) {
		this.fileOffset = fileOffset;
		this.offset = offset;
		this.numInts = numInts;
		chunkLenList = new IntArrayList();
	}
}

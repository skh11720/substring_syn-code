package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ChunkSegmentInfo {
	final int fileOffset;
	final long offset;
	final IntArrayList chunkLenList;
	
	public ChunkSegmentInfo( int fileOffset, long offset ) {
		this.fileOffset = fileOffset;
		this.offset = offset;
		chunkLenList = new IntArrayList();
	}
}

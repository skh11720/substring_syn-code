package snu.kdd.substring_syn.algorithm.index.disk;

public class SegmentInfo {
	final int fileOffset;
	final long offset;
	final int len;
	
	public SegmentInfo( int fileOffset, long offset, int len ) {
		this.fileOffset = fileOffset;
		this.offset = offset;
		this.len = len;
	}
}

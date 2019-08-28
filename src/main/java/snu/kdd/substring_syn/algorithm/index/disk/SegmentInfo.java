package snu.kdd.substring_syn.algorithm.index.disk;

public class SegmentInfo {
	long offset;
	int len;
	
	public SegmentInfo( long offset, int len ) {
		this.offset = offset;
		this.len = len;
	}
}

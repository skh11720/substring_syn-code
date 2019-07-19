package snu.kdd.substring_syn.algorithm.index;

import snu.kdd.substring_syn.data.Record;

public class IndexEntry {

	public final Record rec;
	public final int pos;
	
	public IndexEntry( Record rec, int pos ) {
		this.rec = rec;
		this.pos = pos;
	}
}

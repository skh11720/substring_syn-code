package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public class SubrecordWithPos extends Subrecord {
	
	protected IntList prefixIdxList;

	public SubrecordWithPos(RecordInterface rec, int sidx, int eidx) {
		super(rec, sidx, eidx);
	}
	
	public void setPrefixIdxList( IntList prefixIdxList ) {
		this.prefixIdxList = prefixIdxList;
	}

	public IntList getPrefixIdxList() {
		return prefixIdxList;
	}
}

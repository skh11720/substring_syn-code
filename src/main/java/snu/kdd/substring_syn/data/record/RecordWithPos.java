package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public class RecordWithPos extends Record {

	protected final IntList prefixIdxList;

	public RecordWithPos(int[] tokens, IntList prefixIdxList ) {
		super(tokens);
		this.prefixIdxList = prefixIdxList;
	}

	public IntList getPrefixIdxList() {
		return prefixIdxList;
	}
}

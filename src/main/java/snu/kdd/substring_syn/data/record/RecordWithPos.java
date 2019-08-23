package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public class RecordWithPos extends Record {

	protected final IntList prefixIdxList;
	protected final IntList suffixIdxList;

	public RecordWithPos(int[] tokens, IntList prefixIdxList, IntList suffixIdxList ) {
		super(tokens);
		this.prefixIdxList = prefixIdxList;
		this.suffixIdxList = suffixIdxList;
	}

	public IntList getPrefixIdxList() {
		return prefixIdxList;
	}

	public IntList getSuffixIdxList() {
		return suffixIdxList;
	}
}

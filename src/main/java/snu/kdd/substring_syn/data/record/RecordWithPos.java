package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public class RecordWithPos extends Record {

	protected final IntList prefixIdxList;
	protected final IntList suffixIdxList;

	public RecordWithPos( Record rec, IntList prefixIdxList, IntList suffixIdxList ) {
		super(rec.id, rec.tokens, rec.size);
		this.applicableRules = rec.applicableRules;
		this.suffixApplicableRules = rec.suffixApplicableRules;
		this.transformLengths = rec.transformLengths;
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

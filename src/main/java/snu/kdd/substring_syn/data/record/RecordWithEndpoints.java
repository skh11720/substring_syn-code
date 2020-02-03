package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public class RecordWithEndpoints extends Record {
	
	protected final int sp;
	protected final IntList epList;

	public RecordWithEndpoints( Record rec, int sp, IntList epList ) {
		super(rec.id, rec.tokens, rec.size);
		this.sp = sp;
		this.epList = epList;
		this.applicableRules = rec.applicableRules;
		this.numAppRules = rec.numAppRules;
		this.suffixApplicableRules = rec.suffixApplicableRules;
		this.numSuffixAppRules = rec.numSuffixAppRules;
		this.transformLengths = rec.transformLengths;
		this.suffixRuleLenPairs = rec.suffixRuleLenPairs;
		this.numSuffixRuleLen = rec.numSuffixRuleLen;
		this.maxRhsSize = rec.maxRhsSize;
	}
	
	public int getStartPoint() {
		return sp;
	}

	public IntList getEndpoints() {
		return epList;
	}
}

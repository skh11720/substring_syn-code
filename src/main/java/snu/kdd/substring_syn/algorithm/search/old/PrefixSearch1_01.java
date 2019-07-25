package snu.kdd.substring_syn.algorithm.search.old;

import snu.kdd.substring_syn.algorithm.filter.old.TransSetBoundCalculator1;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.record.Record;

public class PrefixSearch1_01 extends PrefixSearch1_00 {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator1. 
	 */

	public PrefixSearch1_01(double theta) {
		super(theta);
		lf_text = true;
	}

	@Override
	protected void setBoundCalculator( Record rec, double modifiedTheta ) {
		boundCalculator = new TransSetBoundCalculator1(rec, modifiedTheta);
	}

	@Override
	protected boolean applyPrefixFilteringFrom( Record query, Record rec, int widx ) {
		boundCalculator.setStart(widx);
		return super.applyPrefixFilteringFrom(query, rec, widx);
	}

	@Override
	public String getVersion() {
		return "1.01";
	}
}

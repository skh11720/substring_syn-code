package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator1;
import snu.kdd.substring_syn.data.Record;

public class PrefixSearch1_01 extends PrefixSearch {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator1. 
	 */

	public PrefixSearch1_01(double theta) {
		super(theta);
		USE_LF_TEXT_SIDE = true;
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

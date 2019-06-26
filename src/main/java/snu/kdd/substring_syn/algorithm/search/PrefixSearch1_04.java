package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.Record;
import vldb18.PkduckDPEx3;

public class PrefixSearch1_04 extends PrefixSearch1_03 {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator3. 
	 * Use PkduckDPEx3.
	 * Use lbmono in the length filtering.
	 */

	public PrefixSearch1_04( double theta ) {
		super(theta);
	}

	@Override
	protected void setPkduckDP(Record query, Record rec, double modifiedTheta) {
		pkduckdp = new PkduckDPEx3(query, rec, boundCalculator, modifiedTheta);
	}

	@Override
	protected LFOutput applyLengthFiltering( Record query, int widx, int w ) {
		int ub = boundCalculator.getLFUB(widx, widx+w-1);
		int lb = boundCalculator.getLFLB(widx, widx+w-1);
		int lbMono = boundCalculator.getLFLBMono(widx, widx+w-1);
		int qSetSize = query.getDistinctTokenCount();
		if ( qSetSize < lbMono ) return LFOutput.filtered_stop;
		if ( qSetSize > ub ) return LFOutput.filtered_ignore;
		if ( qSetSize < lb ) return LFOutput.filtered_ignore;
		else return LFOutput.not_filtered;
	}

	@Override
	public String getVersion() {
		return "1.04";
	}
}

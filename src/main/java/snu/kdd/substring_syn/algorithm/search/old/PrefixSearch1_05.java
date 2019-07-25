package snu.kdd.substring_syn.algorithm.search.old;

import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator;
import snu.kdd.substring_syn.data.record.Record;
import vldb18.PkduckDPExWIthLF;

public class PrefixSearch1_05 extends PrefixSearch1_04 {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator5. 
	 * Use PkduckDPEx3.
	 * Use lbmono in the length filtering.
	 */

	public PrefixSearch1_05( double theta ) {
		super(theta);
	}

	@Override
	protected void setBoundCalculator(Record rec, double modifiedTheta) {
		statContainer.startWatch("Time_TransSetBoundCalculatorMem");
		if ( lf_text ) boundCalculator = new TransSetBoundCalculator(statContainer, rec, modifiedTheta);
		statContainer.stopWatch("Time_TransSetBoundCalculatorMem");
	}

	@Override
	protected void setPkduckDP(Record query, Record rec, double modifiedTheta) {
		pkduckdp = new PkduckDPExWIthLF(query, rec, boundCalculator, modifiedTheta);
	}

	@Override
	protected LFOutput applyLengthFiltering( Record query, int widx, int w ) {
		int ub = boundCalculator.getLFUB(widx, widx+w-1);
		int lb = boundCalculator.getLFLB(widx, widx+w-1);
		int lbMono = boundCalculator.getLFLBMono(widx, widx+w-1);
		int qSetSize = query.getDistinctTokenCount();
		if ( qSetSize < lbMono ) {
			statContainer.increment("Num_TS_LFByLBMono");
			return LFOutput.filtered_stop;
		}
		if ( qSetSize > ub ) {
			statContainer.increment("Num_TS_LFByUB");
			return LFOutput.filtered_ignore;
		}
		if ( qSetSize < lb ) {
			statContainer.increment("Num_TS_LFByLB");
			return LFOutput.filtered_ignore;
		}
		else return LFOutput.not_filtered;
	}

	@Override
	public String getVersion() {
		return "1.05";
	}
}

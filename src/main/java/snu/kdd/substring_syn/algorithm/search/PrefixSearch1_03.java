package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.Record;
import vldb18.PkduckDPEx3;

public class PrefixSearch1_03 extends PrefixSearch1_02 {
	
	/*
	 * Use length filtering in the text-side transformation.
	 * Use TransSetBoundCalculator3. 
	 * Use PkduckDPEx3.
	 */

	public PrefixSearch1_03( double theta ) {
		super(theta);
	}

	@Override
	protected void setPkduckDP(Record query, Record rec, double modifiedTheta) {
		pkduckdp = new PkduckDPEx3(rec, boundCalculator, modifiedTheta, query.size());
	}

	@Override
	public String getVersion() {
		return "1.03";
	}
}

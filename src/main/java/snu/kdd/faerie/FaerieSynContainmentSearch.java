package snu.kdd.faerie;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSynContainmentSearch extends FaerieSynSearch {

	public FaerieSynContainmentSearch(double theta, boolean isDiskBased) {
		super(theta, isDiskBased);
	}

	@Override
	protected boolean searchRecord( Record query, RecordInterface rec, IntList posList, int minLen, int maxLen, SimCalculator verifier ) {
		return Util.jaccardContainmentM(query.getTokenArray(), rec.getTokenArray()) >= theta;
	}
	
	@Override
	public String getName() {
		return "FaerieSynContainmentSearch";
	}
	
	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}
}

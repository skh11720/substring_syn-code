package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;

public class ZeroRSSearch extends RSSearch {

	public ZeroRSSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bLF, bPF, indexChoice);
	}
	
	@Override
	protected boolean verifyQuerySide(Record query, Subrecord window) {
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return false;
	}
	
	@Override
	protected boolean verifyTextSide(Record query, Subrecord window) {
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return false;
	}

	@Override
	public String getName() {
		return "ZeroRSSearch";
	}
}

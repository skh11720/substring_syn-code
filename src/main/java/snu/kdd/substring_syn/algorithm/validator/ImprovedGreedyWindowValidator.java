package snu.kdd.substring_syn.algorithm.validator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class ImprovedGreedyWindowValidator extends GreedyValidator {

	public ImprovedGreedyWindowValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
	}

	@Override
	public double simTextSide( Record query, RecordInterface window ) {
		int[] transformedText = getTransform(window, query);
//		Log.log.trace("query=%s, window=%s, findBestTransform=%s", ()->query.toOriginalString(), ()->window.toOriginalString(), ()->(new Record(transformedText)).toOriginalString());
		double sim = Util.jaccardM( query.getTokenList(), IntArrayList.wrap(transformedText) );
//		Log.log.trace("sim=%.3f", sim);
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return sim;
	}
}

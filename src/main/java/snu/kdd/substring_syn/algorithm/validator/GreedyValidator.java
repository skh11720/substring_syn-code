package snu.kdd.substring_syn.algorithm.validator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class GreedyValidator extends AbstractGreedyValidator {


	public GreedyValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
	}

	public double simQuerySide( Record query, RecordInterface window ) {
		int[] transformedQuery = getTransform(query, window); 
		double sim = Util.jaccardM( transformedQuery, window.getTokenArray());
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return sim;
	}

	public double simTextSide( Record query, TransformableRecordInterface window ) {
		int[] transformedText = getTransform(window, query);
		double sim = Util.subJaccardM( query.getTokenList(), IntArrayList.wrap(transformedText) );
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return sim;
	}
	
	public int[] getTransform(TransformableRecordInterface trans, RecordInterface target) {
		State state = new State(trans, target);
		state.findBestTransform();
		return state.getTransformedString(trans);
	}

	@Override
	public String getName() {
		return "GreedyValidator";
	}
}

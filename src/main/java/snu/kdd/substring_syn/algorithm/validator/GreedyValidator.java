package snu.kdd.substring_syn.algorithm.validator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class GreedyValidator extends AbstractGreedyValidator {

//	public double simQuerySide( Record query, Record text ) {
//		double simMax = 0;
//		for ( Record queryExp : query.expandAll() ) {
//			double sim = Util.jaccard(queryExp.getTokenArray(), text.getTokenArray());
//			if ( sim > simMax ) log.debug("GreedyValidator.simQuerySide: sim=%.3f, queryExp=%s", sim, queryExp);
//			simMax = Math.max(simMax, sim);
//		}
//		return simMax;
//	}

	public GreedyValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
	}

	public double simQuerySide( Record query, Record window ) {
		State state = new State(query, window);
		state.findBestTransform();
		int[] transformedQuery = state.getTransformedString(query);
		double sim = Util.jaccardM( transformedQuery, window.getTokenArray());
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return sim;
	}

	public double simTextSide( Record query, Record window ) {
		State state = new State(window, query);
		state.findBestTransform();
		int[] transformedText = state.getTransformedString(window);
		double sim = Util.subJaccardM( query.getTokenList(), IntArrayList.wrap(transformedText) );
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return sim;
	}

	@Override
	public String getName() {
		return "GreedyValidator";
	}
}

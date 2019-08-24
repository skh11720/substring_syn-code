package snu.kdd.substring_syn.algorithm.validator;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Log;
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

	public double simQuerySide( Record query, RecordInterface window ) {
		State state = new State(query, window);
		state.findBestTransform();
		int[] transformedQuery = state.getTransformedString(query);
		Log.log.debug("query=%s, window=%s, findBestTransform=%s", ()->query.toOriginalString(), ()->window.toOriginalString(), ()->(new Record(transformedQuery)).toOriginalString());
		double sim = Util.jaccardM( transformedQuery, window.getTokenArray());
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return sim;
	}

	public double simTextSide( Record query, RecordInterface window ) {
		statContainer.startWatch("Time_TS_GreedyValidator.InitState");
		State state = new State(window, query);
		statContainer.stopWatch("Time_TS_GreedyValidator.InitState");
		statContainer.startWatch("Time_TS_GreedyValidator.findBestTransform");
		state.findBestTransform();
		statContainer.stopWatch("Time_TS_GreedyValidator.findBestTransform");
		statContainer.startWatch("Time_TS_GreedyValidator.getTransformedString");
		int[] transformedText = state.getTransformedString(window);
		statContainer.stopWatch("Time_TS_GreedyValidator.getTransformedString");
		Log.log.debug("query=%s, window=%s, findBestTransform=%s", ()->query, ()->window, ()->Arrays.toString(transformedText));
		statContainer.startWatch("Time_TS_GreedyValidator.subJaccardM");
		double sim = Util.subJaccardM( query.getTokenList(), IntArrayList.wrap(transformedText) );
		statContainer.stopWatch("Time_TS_GreedyValidator.subJaccardM");
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return sim;
	}

	@Override
	public String getName() {
		return "GreedyValidator";
	}
}

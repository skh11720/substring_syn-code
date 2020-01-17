package snu.kdd.substring_syn.algorithm.validator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class GreedyValidator extends AbstractGreedyValidator {

//	public double simQuerySide( Record query, Record text ) {
//		double simMax = 0;
//		for ( Record queryExp : query.expandAll() ) {
//			double sim = Util.jaccard(queryExp.getTokenArray(), text.getTokenArray());
//			if ( sim > simMax ) log.trace("GreedyValidator.simQuerySide: sim=%.3f, queryExp=%s", sim, queryExp);
//			simMax = Math.max(simMax, sim);
//		}
//		return simMax;
//	}

	public GreedyValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
	}

	public double simQuerySide( Record query, RecordInterface window ) {
		int[] transformedQuery = getTransform(query, window); 
//		Log.log.trace("query=%s, window=%s, findBestTransform=%s", ()->query.toOriginalString(), ()->window.toOriginalString(), ()->(new Record(transformedQuery)).toOriginalString());
		double sim = Util.jaccardM( transformedQuery, window.getTokenArray());
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return sim;
	}

	public double simTextSide( Record query, RecordInterface window ) {
		int[] transformedText = getTransform(window, query);
//		Log.log.trace("query=%s, window=%s, findBestTransform=%s", ()->query.toOriginalString(), ()->window.toOriginalString(), ()->(new Record(transformedText)).toOriginalString());
		double sim = Util.subJaccardM( query.getTokenList(), IntArrayList.wrap(transformedText) );
//		Log.log.trace("sim=%.3f", sim);
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return sim;
	}
	
	public int[] getTransform(RecordInterface trans, RecordInterface target) {
		State state = new State(trans, target);
		state.findBestTransform();
		return state.getTransformedString(trans);
	}

	@Override
	public String getName() {
		return "GreedyValidator";
	}
}

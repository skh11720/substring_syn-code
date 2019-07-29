package snu.kdd.substring_syn.algorithm.verify;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.algorithm.validator.AbstractGreedyValidator;
import snu.kdd.substring_syn.data.record.Record;
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

	public double simQuerySide( Record query, Record text ) {
		State state = new State(query, text);
		state.findBestTransform();
		int[] transformedQuery = state.getTransformedString(query);
		double sim = Util.jaccardM( transformedQuery, text.getTokenArray());
		return sim;
	}

	public double simTextSide( Record query, Record text ) {
		State state = new State(text, query);
		state.findBestTransform();
		int[] transformedText = state.getTransformedString(text);
		double sim = Util.subJaccardM( query.getTokenList(), IntArrayList.wrap(transformedText) );
		return sim;
	}

	@Override
	public String getName() {
		return "GreedyValidator";
	}
}

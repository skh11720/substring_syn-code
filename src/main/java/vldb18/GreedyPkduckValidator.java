package vldb18;

import snu.kdd.substring_syn.algorithm.validator.AbstractGreedyValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class GreedyPkduckValidator extends AbstractGreedyValidator {
	
	public GreedyPkduckValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
	}

	public double sim( Record x, Record y ) {
		/*
		 * TODO:  call sim2y with y and x
		 */
		if ( areSameString(x, y) ) return 1;
		else return simx2y(x, y);
	}
	
	public boolean isSimx2yOverThreahold(Record x, Record y, double theta) {
		return simx2y(x, y) >= theta;
	}
	
	private double simx2y( Record x, Record y ) {
		State state = new State(x, y);
		state.findBestTransform();
		int[] transformedRecord = state.getTransformedString(x);
		double sim = Util.jaccard( transformedRecord, y.getTokenArray());
		return sim;
	}
	
	@Override
	public String getName() {
		return "GreedyPkduckValidator";
	}
}

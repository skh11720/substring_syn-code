package snu.kdd.substring_syn.algorithm.validator;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Util;

public class GreedySimTFValidator extends AbstractGreedyValidator {
	
	// TODO: test this validator

	@Override
	public boolean isSimx2yOverThreahold(Record x, Record y, double theta) {
		return simx2y(x, y) >= theta;
	}

	private double simx2y( Record x, Record y ) {
		State state = new State(x, y);
		state.findBestTransform();
		int[] transformedRecord = state.getTransformedString(x);
		double sim = Util.subJaccard( y.getTokenArray(), transformedRecord );
		return sim;
	}

	@Override
	public String getName() {
		return "GreedySimTFValidator";
	}
}

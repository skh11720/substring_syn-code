package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.validator.ImprovedGreedyWindowValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class SimWPositionRSSearch extends PositionRSSearch {

	protected final ImprovedGreedyWindowValidator validator;

	
	public SimWPositionRSSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bLF, bPF, indexChoice);
		validator = new ImprovedGreedyWindowValidator(theta, statContainer);
	}

	@Override
	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		return validator.simQuerySide(query, window) >= theta;
	}

	@Override
	protected boolean verifyTextSide( Record query, Subrecord window ) {
		Record rec = window.toRecord();
		rec.preprocessAll();
		return validator.simTextSide(query, window) >= theta;
	}

	@Override
	public String getName() {
		return "SimWPositionRSSearch";
	}
	
	@Override
	public String getVersion() {
		/*
		 * 1.00: version
		 */
		return "1.00";
	}
}

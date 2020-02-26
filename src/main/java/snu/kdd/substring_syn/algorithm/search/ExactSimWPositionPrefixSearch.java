package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.validator.NaiveWindowBasedValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class ExactSimWPositionPrefixSearch extends PositionPrefixSearch {

	protected final NaiveWindowBasedValidator validator;

	
	public ExactSimWPositionPrefixSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bLF, bPF, indexChoice);
		validator = new NaiveWindowBasedValidator(theta, statContainer);
	}

	@Override
	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		return validator.isOverThresholdQuerySide(query, window);
	}

	@Override
	protected boolean verifyTextSide( Record query, Subrecord window ) {
		Record rec = window.toRecord();
		rec.preprocessAll();
		return validator.isOverThresholdTextSide(query, rec);
	}

	@Override
	public String getName() {
		return "ExactSimWPositionPrefixSearch";
	}
}

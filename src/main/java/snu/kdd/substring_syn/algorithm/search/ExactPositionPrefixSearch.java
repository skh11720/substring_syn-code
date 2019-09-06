package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import vldb18.NaivePkduckValidator;

public class ExactPositionPrefixSearch extends PositionPrefixSearch {

	protected final NaivePkduckValidator validator;

	
	public ExactPositionPrefixSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bLF, bPF, indexChoice);
		validator = new NaivePkduckValidator(theta, statContainer);
	}

	@Override
	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		return validator.verifyQuerySide(query, window, theta);
	}

	@Override
	protected boolean verifyTextSide( Record query, Subrecord window ) {
		return validator.verifyTextSide(query, window, theta);
	}

	@Override
	public String getName() {
		return "ExactPrefixSearch";
	}
}

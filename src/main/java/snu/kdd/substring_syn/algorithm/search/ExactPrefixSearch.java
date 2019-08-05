package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import vldb18.NaivePkduckValidator;

public class ExactPrefixSearch extends PrefixSearch {

	protected final NaivePkduckValidator validator;

	
	public ExactPrefixSearch( double theta, boolean bIF, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bIF, bLF, bPF, indexChoice);
		validator = new NaivePkduckValidator(theta, statContainer);
	}

	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		return validator.verifyQuerySide(query, window.toRecord(), theta);
	}

	protected boolean verifyTextSide( Record query, Subrecord window ) {
		return validator.verifyTextSide(query, window.toRecord(), theta);
	}

	@Override
	public String getName() {
		return "ExactPrefixSearch";
	}
}

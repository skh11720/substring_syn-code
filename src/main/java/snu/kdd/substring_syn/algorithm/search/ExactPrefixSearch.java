package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import vldb18.NaivePkduckValidator;

public class ExactPrefixSearch extends PrefixSearch {

	protected final NaivePkduckValidator validator;

	
	public ExactPrefixSearch( double theta, boolean idxFilter_query, boolean idxFilter_text, boolean lf_query, boolean lf_text, IndexChoice indexChoice ) {
		super(theta, idxFilter_query, idxFilter_text, lf_text, lf_text, indexChoice);
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

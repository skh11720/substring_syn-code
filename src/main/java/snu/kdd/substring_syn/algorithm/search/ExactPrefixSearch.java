package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import vldb18.NaivePkduckValidator;

public class ExactPrefixSearch extends PrefixSearch {

	protected final NaivePkduckValidator validator;

	
	public ExactPrefixSearch( double theta, boolean idxFilter_query, boolean idxFilter_text, boolean lf_query, boolean lf_text ) {
		super(theta, idxFilter_query, idxFilter_text, lf_text, lf_text);
		validator = new NaivePkduckValidator();
	}

	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		return validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
	}

	protected boolean verifyTextSide( Record query, Subrecord window ) {
		return validator.isSimx2yOverThreahold(window.toRecord(), query, theta);
	}

	@Override
	public String getName() {
		return "ExactPrefixSearch";
	}
}

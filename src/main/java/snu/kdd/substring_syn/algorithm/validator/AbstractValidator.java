package snu.kdd.substring_syn.algorithm.validator;

import snu.kdd.substring_syn.data.Record;

public abstract class AbstractValidator {

	protected boolean areSameString( Record x, Record y ) {
		return x.equals(y);
	}

	public abstract boolean isSimx2yOverThreahold( Record x, Record y, double theta );

	public abstract String getName();
}

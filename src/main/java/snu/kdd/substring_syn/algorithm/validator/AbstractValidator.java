package snu.kdd.substring_syn.algorithm.validator;

import java.util.Arrays;

import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractValidator {

	protected final double theta;
	protected final StatContainer statContainer;

	public AbstractValidator( double theta, StatContainer statContainer ) {
		this.theta = theta;
		this.statContainer = statContainer;
	}


	protected boolean areSameString( RecordInterface x, RecordInterface y ) {
		return Arrays.equals(x.getTokenArray(), y.getTokenArray());
	}

//	public abstract boolean isSimx2yOverThreahold( Record x, Record y, double theta );

	public abstract String getName();
}

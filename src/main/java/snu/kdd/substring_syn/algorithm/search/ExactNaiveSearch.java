package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class ExactNaiveSearch extends AbstractSearch {
	
	NaiveValidator validator;


	public ExactNaiveSearch(double theta) {
		super(theta);
		validator = new NaiveValidator(theta, statContainer);
	}

	@Override
	protected void searchRecordQuerySide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.startWatch(Stat.Time_QS_Validation);
		boolean isSim = validator.isOverThresholdQuerySide(query, rec); 
		statContainer.stopWatch(Stat.Time_QS_Validation);
		if ( isSim ) {
			Log.log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
			addResultQuerySide(query, rec);
			return;
		}
	}
	
	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.startWatch(Stat.Time_TS_Validation);
		boolean isSim = validator.isOverThresholdTextSide(query, rec);
		statContainer.stopWatch(Stat.Time_TS_Validation);
		if ( isSim ) {
			Log.log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
			addResultTextSide(query, rec);
			return;
		}
	}

	@Override
	public String getName() {
		return "ExactNaiveSearch";
	}

	@Override
	public String getVersion() {
		/*
		 * 3.00: multiset
		 * 4.00: refactor
		 * 4.01: rename: NaiveSearch -> ExactNaiveSearch
		 */
		return "4.01";
	}
}

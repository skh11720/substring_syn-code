package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class ExactNaiveSearch extends AbstractSearch {
	
	NaiveValidator validator;


	public ExactNaiveSearch(double theta) {
		super(theta);
		validator = new NaiveValidator(theta, statContainer);
	}

	@Override
	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		Log.log.trace("searchRecordFromQuery(%d, %d)", ()->query.getIdx(), ()->rec.getIdx());
		statContainer.startWatch(Stat.Time_QS_Validation);
		boolean isSim = validator.isOverThresholdQuerySide(query, rec); 
		statContainer.stopWatch(Stat.Time_QS_Validation);
		if ( isSim ) {
			Log.log.trace("rsltFromQuery.add(%d, %d)", ()->query.getIdx(), ()->rec.getIdx());
			addResultQuerySide(query, rec);
			return;
		}
	}
	
	@Override
	protected void searchRecordTextSide( Record query, TransformableRecordInterface rec ) {
		Log.log.trace("searchRecordFromText(%d, %d)", ()->query.getIdx(), ()->rec.getIdx());
		statContainer.startWatch(Stat.Time_TS_Validation);
		boolean isSim = validator.isOverThresholdTextSide(query, rec);
		statContainer.stopWatch(Stat.Time_TS_Validation);
		if ( isSim ) {
			Log.log.trace("rsltFromText.add(%d, %d)", ()->query.getIdx(), ()->rec.getIdx());
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
		return "4.01";
	}
}

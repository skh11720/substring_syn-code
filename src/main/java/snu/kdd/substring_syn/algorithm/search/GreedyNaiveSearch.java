package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.algorithm.validator.ImprovedGreedyValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.window.SortedWindowExpander;

public class GreedyNaiveSearch extends AbstractSearch {

	GreedyValidator validator;

	public GreedyNaiveSearch(double theta) {
		super(theta);
		validator = new ImprovedGreedyValidator(theta, statContainer);
	}

	@Override
	protected void searchRecordQuerySide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				statContainer.startWatch(Stat.Time_QS_Validation);
				double sim = validator.simQuerySide(query, window);
				statContainer.stopWatch(Stat.Time_QS_Validation);
				if ( sim >= theta ) {
					Log.log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					addResultQuerySide(query, rec);
					return;
				}
			}
		}
	}
	
	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				statContainer.startWatch(Stat.Time_TS_Validation);
				double sim = validator.simTextSide(query, window);
				statContainer.stopWatch(Stat.Time_TS_Validation);
				if ( sim >= theta ) {
					Log.log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					addResultTextSide(query, rec);
					return;
				}
			}
		}
	}

	@Override
	public String getName() {
		return "GreedyNaiveSearch";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: Initial version
		 */
		return "1.00";
	}

}

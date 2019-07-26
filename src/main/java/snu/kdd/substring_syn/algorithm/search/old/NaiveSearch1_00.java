package snu.kdd.substring_syn.algorithm.search.old;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;
import vldb18.NaivePkduckValidator;

@Deprecated
public class NaiveSearch1_00 extends AbstractSearch {

	final NaivePkduckValidator validator;


	public NaiveSearch1_00(double theta) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Len_QS_Searched, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				statContainer.startWatch(Stat.Time_Validation);
				boolean isSim = validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
				statContainer.stopWatch(Stat.Time_Validation);
				statContainer.increment(Stat.Num_QS_Verified);
				statContainer.addCount(Stat.Len_QS_Verified, window.size());
				if (isSim) {
					Log.log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
					return;
				}
			}
		}
	}
	
	protected void searchRecordTextSide( Record query, RecordInterface rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Len_TS_Searched, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				Log.log.trace("w=%d, widx=%d", w, window.sidx);
				statContainer.startWatch(Stat.Time_Validation);
				boolean isSim = validator.isSimx2yOverThreahold(window.toRecord(), query, theta);
				statContainer.stopWatch(Stat.Time_Validation);
				statContainer.increment(Stat.Num_TS_Verified);
				statContainer.addCount(Stat.Len_TS_Verified, window.size());
				if (isSim) {
					Log.log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
					return;
				}
			}
		}
	}
	
	@Override
	public String getName() {
		return "NaiveSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

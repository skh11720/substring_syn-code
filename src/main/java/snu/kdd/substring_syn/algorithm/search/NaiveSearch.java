package snu.kdd.substring_syn.algorithm.search;

import java.util.ArrayList;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;

public class NaiveSearch extends AbstractSearch {


	public NaiveSearch(double theta) {
		super(theta);
	}

	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		ArrayList<Record> queryExpArr = query.expandAll();
		statContainer.addCount(Stat.Num_QS_WindowSizeAll, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				statContainer.addCount(Stat.Num_QS_WindowSizeVerified, w);
				Subrecord window = witer.next();
				for ( Record queryExp : queryExpArr ) {
					statContainer.startWatch(Stat.Time_3_Validation);
					double sim = Util.jaccard(queryExp.getTokenArray(), window.getTokenArray());
					statContainer.stopWatch(Stat.Time_3_Validation);
					statContainer.increment(Stat.Num_QS_Verified);
					if ( sim >= theta ) {
						Log.log.debug("rsltFromQuery.add(%d, %d), sim=%.3f", ()->query.getID(), ()->rec.getID(), ()->sim);
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						return;
					}
				}
			}
		}
	}
	
	protected void searchRecordTextSide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( Record exp : rec.expandAll() ) {
			statContainer.addCount(Stat.Num_TS_WindowSizeAll, Util.sumWindowSize(exp));
			for ( int w=1; w<=exp.size(); ++w ) {
				SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(exp, w, theta);
				while ( witer.hasNext() ) {
					statContainer.addCount(Stat.Num_TS_WindowSizeVerified, w);
					Subrecord window = witer.next();
					Log.log.trace("w=%d, widx=%d", w, window.sidx);
					statContainer.startWatch(Stat.Time_3_Validation);
					double sim = Util.jaccard(window.getTokenArray(), query.getTokenArray());
					statContainer.stopWatch(Stat.Time_3_Validation);
					statContainer.increment(Stat.Num_TS_Verified);
					if ( sim >= theta ) {
						Log.log.debug("rsltFromText.add(%d, %d), sim=%.3f", ()->query.getID(), ()->rec.getID(), ()->sim);
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						return;
					}
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
		return "2.00";
	}
}

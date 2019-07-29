package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
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
		ObjectList<Record> queryExpArr = Records.expandAll(query);
		statContainer.addCount(Stat.Len_QS_Searched, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				for ( Record queryExp : queryExpArr ) {
					statContainer.startWatch(Stat.Time_Validation);
					double sim = Util.jaccard(queryExp.getTokenArray(), window.getTokenArray());
					statContainer.stopWatch(Stat.Time_Validation);
					statContainer.increment(Stat.Num_QS_Verified);
					statContainer.addCount(Stat.Len_QS_Verified, window.size());
					if ( sim >= theta ) {
						Log.log.debug("rsltFromQuery.add(%d, %d), sim=%.3f", ()->query.getID(), ()->rec.getID(), ()->sim);
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						return;
					}
				}
			}
		}
	}
	
	protected void searchRecordTextSide( Record query, RecordInterface rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( Record exp : Records.expandAll(rec) ) {
			statContainer.addCount(Stat.Len_TS_Searched, Util.sumWindowSize(exp));
			for ( int w=1; w<=exp.size(); ++w ) {
				SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(exp, w, theta);
				while ( witer.hasNext() ) {
					Subrecord window = witer.next();
					Log.log.trace("w=%d, widx=%d", w, window.sidx);
					statContainer.startWatch(Stat.Time_Validation);
					double sim = Util.jaccard(window.getTokenArray(), query.getTokenArray());
					statContainer.stopWatch(Stat.Time_Validation);
					statContainer.increment(Stat.Num_TS_Verified);
					statContainer.addCount(Stat.Len_TS_Verified, window.size());
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

package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindowIterator;
import vldb18.NaivePkduckValidator;

public class NaiveSearch extends AbstractSearch {

	final NaivePkduckValidator validator;


	public NaiveSearch(double theta) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	protected void searchQuerySide( Record query, Record rec ) {
		log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				Subrecord window = witer.next();
				statContainer.startWatch(Stat.Time_1_Validation);
				boolean isSim = validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
				statContainer.stopWatch(Stat.Time_1_Validation);
				statContainer.increment(Stat.Num_VerifyQuerySide);
				if (isSim) {
					log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
					return;
				}
			}
		}
	}
	
	protected void searchTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				Subrecord window = witer.next();
				statContainer.startWatch(Stat.Time_1_Validation);
				boolean isSim = validator.isSimx2yOverThreahold(window.toRecord(), query, theta);
				statContainer.stopWatch(Stat.Time_1_Validation);
				statContainer.increment(Stat.Num_VerifyTextSide);
				if (isSim) {
					log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
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

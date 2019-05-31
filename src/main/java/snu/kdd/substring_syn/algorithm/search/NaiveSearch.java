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
			for ( int widx=0; witer.hasNext(); ++widx ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				Subrecord window = witer.next();
				double sim = verifyQuerySide(query, window);
				log.trace("FromQuery query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
				if ( sim >= theta ) {
					log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					rsltFromQuery.add(new IntPair(query.getID(), rec.getID()));
					return;
				}
			}
		}
	}
	
	protected double verifyQuerySide( Record query, Subrecord window ) {
		statContainer.startWatch(Stat.Time_1_Validation);
		double sim = validator.simx2y(query, window.toRecord());
		statContainer.stopWatch(Stat.Time_1_Validation);
		statContainer.increment(Stat.Num_VerifyQuerySide);
		return sim;
	}
	
	protected void searchTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			for ( int widx=0; witer.hasNext(); ++widx ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				Subrecord window = witer.next();
				double sim = verifyTextSide(query, window);
				log.trace("FromText query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
				if ( sim >= theta ) {
					log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					rsltFromText.add(new IntPair(query.getID(), rec.getID()));
					return;
				}
			}
		}
	}
	
	protected double verifyTextSide( Record query, Subrecord window ) {
		statContainer.startWatch(Stat.Time_1_Validation);
		double sim = validator.simx2y(window.toRecord(), query);
		statContainer.stopWatch(Stat.Time_1_Validation);
		statContainer.increment(Stat.Num_VerifyTextSide);
		return sim;
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

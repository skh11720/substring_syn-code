package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
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
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			for ( int widx=0; witer.hasNext(); ++widx ) {
				Subrecord window = witer.next();
				double sim = validator.simx2y(query, window.toRecord());
				log.trace("FromQuery query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
				if ( sim >= theta ) {
					rsltFromQuery.add(new IntPair(query.getID(), rec.getID()));
					log.debug("rsltFromQuery.add(%d, %d)", ()->query.getID(), ()->rec.getID());
					return;
				}
			}
		}
	}
	
	protected void searchTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			for ( int widx=0; witer.hasNext(); ++widx ) {
				Subrecord window = witer.next();
				double sim = validator.simx2y(window.toRecord(), query);
				log.trace("FromText query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
				if ( sim >= theta ) {
					rsltFromText.add(new IntPair(query.getID(), rec.getID()));
					log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
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

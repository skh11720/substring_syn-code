package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindow;
import vldb18.NaivePkduckValidator;

public class NaiveSearch extends AbstractSearch {

	final NaivePkduckValidator validator;


	public NaiveSearch(double theta) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	protected void searchRecordFromQuery( Record query, Record rec ) {
		log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
	}
	
	protected void searchRecordFromText( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindow windowSlider = new RecordSortedSlidingWindow(rec, w, theta);
			for ( int widx=0; windowSlider.hasNext(); ++widx ) {
				Subrecord window = windowSlider.next();
				double sim = validator.sim(query, window.toRecord());
				log.debug("query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
				if ( sim >= theta ) {
					log.debug("query: %s", ()->query);
					log.debug("window: %s", ()->window.toString());
					log.debug("query_prefix: %s", ()->Util.getPrefix(query, theta));
					log.debug("window_prefix: %s", ()->Util.getPrefix(window.toRecord(), theta));
					log.debug("rec: %s", ()->rec.toStringDetails());
					rsltFromText.add(new IntPair(query.getID(), rec.getID()));
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

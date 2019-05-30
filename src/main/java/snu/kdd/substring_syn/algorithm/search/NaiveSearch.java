package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindowIterator;
import vldb18.NaivePkduckValidator;
import vldb18.PkduckDP;

public class NaiveSearch extends AbstractSearch {

	final NaivePkduckValidator validator;


	public NaiveSearch(double theta) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	protected void searchRecordFromQuery( Record query, Record rec ) {
		log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		IntSet expandedPrefix = getExpandedPrefix(query);
//		int[] wrange = getRangeOfWindowSize(query);
//		for ( int w=wrange[0]; w<=wrange[1]; ++w ) {
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			for ( int widx=0; witer.hasNext(); ++widx ) {
				Subrecord window = witer.next();
				IntSet wprefix = witer.getPrefix();
				if (Util.hasIntersection(wprefix, expandedPrefix)) {
					double sim = validator.simx2y(query, window.toRecord());
					log.debug("FromQuery query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
					if ( sim >= theta ) {
						rsltFromQuery.add(new IntPair(query.getID(), rec.getID()));
						return;
					}
				}
			}
		}
	}
	
	protected IntSet getExpandedPrefix( Record query ) {
		IntSet candTokenSet = query.getCandTokenSet();
		IntSet expandedPrefix = new IntOpenHashSet();
		PkduckDP pkduckdp = new PkduckDP(query, theta);
		for ( int target : candTokenSet ) {
			if ( pkduckdp.isInSigU(target) ) expandedPrefix.add(target);
		}
		return expandedPrefix;
	}
	
	protected int[] getRangeOfWindowSize( Record query ) {
		int[] range = new int[2];
		range[0] = (int)Math.ceil(query.getMinTransLength()*theta);
		range[1] = (int)Math.floor(query.getMaxTransLength()/theta);
		return range;
	}


	
	protected void searchRecordFromText( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
			for ( int widx=0; witer.hasNext(); ++widx ) {
				Subrecord window = witer.next();
				double sim = validator.simx2y(window.toRecord(), query);
				log.debug("FromText query=%d, rec=%d, w=%d, widx=%d, sim=%.3f", query.getID(), rec.getID(), w, widx, sim);
				if ( sim >= theta ) {
					log.debug("query: %s", ()->query);
					log.debug("window: %s", ()->window.toString());
					log.debug("query_prefix: %s", ()->Util.getPrefix(query, theta));
					log.debug("window_prefix: %s", ()->Util.getExpandedPrefix(window.toRecord(), theta));
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

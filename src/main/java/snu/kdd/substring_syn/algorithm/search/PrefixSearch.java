package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Records;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;
import vldb18.NaivePkduckValidator;
import vldb18.PkduckDP;
import vldb18.PkduckDPEx;

public class PrefixSearch extends AbstractSearch {

	public static boolean USE_LF_QUERY_SIDE = true;
	final NaivePkduckValidator validator;
	private IntRange wRange;

	
	public PrefixSearch( double theta ) {
		super(theta);
		validator = new NaivePkduckValidator();
	}
	
	@Override
	protected void prepareSearch( Record query ) {
		log.debug("prepareSearch(%d)",  ()->query.getID());
		wRange = getWindowSizeRangeQuerySide(query);
		log.debug("wRange=(%d,%d)", wRange.min, wRange.max);
	}

	@Override
	protected void searchQuerySide( Record query, Record rec ) {
		log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		IntSet expandedPrefix = getExpandedPrefix(query);
		for ( int w=wRange.min; w<=wRange.max; ++w ) {
			SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				Subrecord window = witer.next();
				IntSet wprefix = witer.getPrefix();
				if (Util.hasIntersection(wprefix, expandedPrefix)) {
					statContainer.startWatch(Stat.Time_1_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
					statContainer.stopWatch(Stat.Time_1_Validation);
					statContainer.increment(Stat.Num_VerifyQuerySide);
					if (isSim) {
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						log.debug("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
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
	
	protected IntRange getWindowSizeRangeQuerySide( Record rec ) {
		if (USE_LF_QUERY_SIDE) {
			int lb = Records.getTransSetSizeLowerBound(rec);
			int min = (int)Math.max(1.0, theta*lb);
			int max = Math.min(rec.getMaxTransLength(), rec.size());
			return new IntRange(min, max);
		}
		else return new IntRange(1, rec.size());
	}
	
	@Override
	protected void searchTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_WindowSizeAll, Util.sumWindowSize(rec));
		IntList candTokenList = getCandTokenList(query, rec);
		PkduckDPEx pkduckdp = new PkduckDPEx(rec, theta, query.size());
		for ( int target : candTokenList ) {
			long ts0 = System.nanoTime();
			pkduckdp.compute(target);
			log.trace("PkduckDPEx.compute(%d) rec.id=%d  %.3f ms", target, rec.getID(), (System.nanoTime()-ts0)/1e6);
			ts0 = System.nanoTime();
			boolean isSimilar = applyPrefixFiltering(query, rec, pkduckdp, target);
			if ( isSimilar ) break;
			log.trace("PrefixSearch.applyPrefixFiltering(...)  %4d/%4d  %.3f ms", 0, rec.size()*(rec.size()+1)/2, (System.nanoTime()-ts0)/1e6 );
		}
	}
	
	protected IntList getCandTokenList( Record query, Record rec ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(Util.getPrefix(query, theta));
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected boolean applyPrefixFiltering( Record query, Record rec, PkduckDPEx pkduckdp, int target ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			IntRange wRange = new IntRange(1, rec.size());
			for ( int w=wRange.min; w<=wRange.max; ++w ) {
				statContainer.increment(Stat.Num_VerifiedWindowSize);
				boolean isInSigU = pkduckdp.isInSigU(widx, w);
				log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  widx=%d/%d  w=%d/%d  isInSigU=%s", query.getID(), rec.getID(), target, widx, rec.size()-1, w, rec.size(), isInSigU );
				if ( isInSigU ) {
					Subrecord window = new Subrecord(rec, widx, widx+w);
					statContainer.startWatch(Stat.Time_1_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(window.toRecord(), query, theta);
					statContainer.stopWatch(Stat.Time_1_Validation);
					statContainer.increment(Stat.Num_VerifyTextSide);
					if (isSim) {
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						log.debug("rsltFromText.add(%d, %d)", ()->query.getID(), ()->rec.getID());
						return true;
					}
				}
			}
		}
		log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  isInSigU=false", query.getID(), rec.getID(), target);
		return false;
	}

	@Override
	public String getName() {
		return "PrefixSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

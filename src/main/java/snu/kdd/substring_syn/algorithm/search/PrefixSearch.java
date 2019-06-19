package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator;
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
	public static boolean USE_LF_TEXT_SIDE = true;
	final NaivePkduckValidator validator;

	
	public PrefixSearch( double theta ) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	@Override
	protected void searchQuerySide( Record query, Record rec ) {
		log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_QS_WindowSizeAll, Util.sumWindowSize(rec));
		IntSet expandedPrefix = getExpandedPrefix(query);
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		log.debug("wRange=(%d,%d)", wRange.min, wRange.max);
		for ( int w=wRange.min; w<=wRange.max; ++w ) {
			SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( witer.hasNext() ) {
				statContainer.addCount(Stat.Num_QS_WindowSizeLF, w);
				Subrecord window = witer.next();
				IntSet wprefix = witer.getPrefix();
				if (Util.hasIntersection(wprefix, expandedPrefix)) {
					statContainer.addCount(Stat.Num_QS_WindowSizeVerified, w);
					statContainer.startWatch(Stat.Time_3_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(query, window.toRecord(), theta);
					statContainer.stopWatch(Stat.Time_3_Validation);
					statContainer.increment(Stat.Num_QS_Verified);
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
	
	protected IntRange getWindowSizeRangeQuerySide( Record query, Record rec ) {
		if (USE_LF_QUERY_SIDE) {
			int lb = Records.getTransSetSizeLowerBound(query);
			int min = Math.max(1, (int)Math.ceil(theta*lb));
			int max = (int)Math.min(1.0*query.getMaxTransLength()/theta, rec.size());
			return new IntRange(min, max);
		}
		else return new IntRange(1, rec.size());
	}
	
	@Override
	protected void searchTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_TS_WindowSizeAll, Util.sumWindowSize(rec));
		double modifiedTheta = getModifiedTheta(query, rec);
		IntList candTokenList = getCandTokenList(query, rec, modifiedTheta);
		TransSetBoundCalculator boundCalculator = new TransSetBoundCalculator(rec, modifiedTheta);
		PkduckDPEx pkduckdp = new PkduckDPEx(rec, modifiedTheta, query.size());
		for ( int target : candTokenList ) {
			long ts0 = System.nanoTime();
			pkduckdp.compute(target);
			log.trace("PkduckDPEx.compute(%d) rec.id=%d  %.3f ms", target, rec.getID(), (System.nanoTime()-ts0)/1e6);
			ts0 = System.nanoTime();
			boolean isSimilar = applyPrefixFiltering(query, rec, pkduckdp, boundCalculator, target);
			if ( isSimilar ) break;
			log.trace("PrefixSearch.applyPrefixFiltering(...)  %4d/%4d  %.3f ms", 0, rec.size()*(rec.size()+1)/2, (System.nanoTime()-ts0)/1e6 );
		}
	}
	
	protected IntList getCandTokenList( Record query, Record rec, double theta ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(Util.getPrefix(query, theta));
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected boolean applyPrefixFiltering( Record query, Record rec, PkduckDPEx pkduckdp, TransSetBoundCalculator boundCalculator, int target ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			boundCalculator.setStart(widx);
			for ( int w=1; w<=rec.size()-widx; ++w ) {
				int ub = boundCalculator.getNextUB();
				int lb = boundCalculator.getNextLB();
				boolean isInSigU = pkduckdp.isInSigU(widx, w);
				log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  widx=%d/%d  w=%d/%d  isInSigU=%s", query.getID(), rec.getID(), target, widx, rec.size()-1, w, rec.size(), isInSigU );
				if ( USE_LF_TEXT_SIDE && query.getDistinctTokenCount() > ub ) continue;
				if ( USE_LF_TEXT_SIDE && query.getDistinctTokenCount() < lb ) continue;
				statContainer.addCount(Stat.Num_TS_WindowSizeLF, w);
				if ( isInSigU ) {
					statContainer.addCount(Stat.Num_TS_WindowSizeVerified, w);
					Subrecord window = new Subrecord(rec, widx, widx+w);
					statContainer.startWatch(Stat.Time_3_Validation);
					boolean isSim = validator.isSimx2yOverThreahold(window.toRecord(), query, theta);
					statContainer.stopWatch(Stat.Time_3_Validation);
					statContainer.increment(Stat.Num_TS_Verified);
					if (isSim) {
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						log.debug("rsltFromText.add(%d, %d), w=%d, widx=%d", query.getID(), rec.getID(), w, widx);
						return true;
					}
				}
			}
		}
		log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, target=%d, ...)  isInSigU=false", query.getID(), rec.getID(), target);
		return false;
	}
	
	protected double getModifiedTheta( Record query, Record rec ) {
		return theta * query.size() / (query.size() + 2*(rec.getMaxRhsSize()-1));
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

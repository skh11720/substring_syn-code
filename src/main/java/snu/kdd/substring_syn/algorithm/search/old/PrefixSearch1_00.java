package snu.kdd.substring_syn.algorithm.search.old;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.algorithm.filter.old.TransSetBoundCalculator1;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
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

public class PrefixSearch1_00 extends AbstractSearch {

	protected boolean lf_query = true;
	protected boolean lf_text = false;
	protected final NaivePkduckValidator validator;
	protected TransSetBoundCalculator1 boundCalculator = null;
	protected PkduckDPEx pkduckdp = null;

	
	public PrefixSearch1_00( double theta ) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	@Override
	protected void searchRecordQuerySide( Record query, Record rec ) {
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
		if (lf_query) {
			int lb = query.getTransSetLB();
			int min = Math.max(1, (int)Math.ceil(theta*lb));
			int max = (int)Math.min(1.0*query.getMaxTransLength()/theta, rec.size());
			return new IntRange(min, max);
		}
		else return new IntRange(1, rec.size());
	}
	
	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
		log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_TS_WindowSizeAll, Util.sumWindowSize(rec));
		double modifiedTheta = getModifiedTheta(query, rec);
		IntList candTokenList = getCandTokenList(query, rec, modifiedTheta);
		setBoundCalculator(rec, modifiedTheta);
		setPkduckDP(query, rec, modifiedTheta);
		for ( int target : candTokenList ) {
			statContainer.startWatch("Time_TS_Pkduck");
			pkduckdp.compute(target);
			statContainer.stopWatch("Time_TS_Pkduck");
			boolean isSimilar = applyPrefixFiltering(query, rec);
			if ( isSimilar ) break;
		}
	}
	
	protected void setBoundCalculator( Record rec, double modifiedTheta ) {
		boundCalculator = null;
	}
	
	protected void setPkduckDP( Record query, Record rec, double modifiedTheta ) {
		pkduckdp = new PkduckDPEx(query, rec, modifiedTheta);
	}
	
	protected IntList getCandTokenList( Record query, Record rec, double theta ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(Util.getPrefix(query, theta));
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected boolean applyPrefixFiltering( Record query, Record rec ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			if ( applyPrefixFilteringFrom(query, rec, widx) ) return true;
		}
		log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, ...)  isInSigU=false", query.getID(), rec.getID());
		return false;
	}
	
	protected boolean applyPrefixFilteringFrom( Record query, Record rec, int widx ) {
		for ( int w=1; w<=rec.size()-widx; ++w ) {
			if ( lf_text ) {
				LFOutput lfOutput = applyLengthFiltering(query);
				if ( lfOutput == LFOutput.filtered_ignore ) continue;
				else if ( lfOutput == LFOutput.filtered_stop ) break;
			}
			statContainer.addCount(Stat.Num_TS_WindowSizeLF, w);
			if ( applyPrefixFilteringToWindow(query, rec, widx, w) ) return true;
		}
		return false;
	}
	
	protected boolean applyPrefixFilteringToWindow( Record query, Record rec, int widx, int w ) {
		log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, ...)  widx=%d/%d  w=%d/%d", query.getID(), rec.getID(), widx, rec.size()-1, w, rec.size());
		boolean isInSigU = pkduckdp.isInSigU(widx, w);
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
		return false;
	}
	
	protected enum LFOutput {
		filtered_ignore,
		filtered_stop,
		not_filtered
	}
	
	protected LFOutput applyLengthFiltering( Record query ) {
		int ub = boundCalculator.getNextUB();
		int lb = boundCalculator.getNextLB();
		if ( query.getDistinctTokenCount() > ub ) return LFOutput.filtered_ignore;
		if ( query.getDistinctTokenCount() < lb ) return LFOutput.filtered_ignore;
		return LFOutput.not_filtered;
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

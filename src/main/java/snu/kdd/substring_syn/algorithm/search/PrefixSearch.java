package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculatorInterface;
import snu.kdd.substring_syn.algorithm.index.AbstractIndexBasedFilter;
import snu.kdd.substring_syn.algorithm.index.NaiveIndexBasedFilter;
import snu.kdd.substring_syn.algorithm.index.PositionalIndexBasedFilter;
import snu.kdd.substring_syn.algorithm.verify.GreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.SortedWindowExpander;
import vldb18.PkduckDP;
import vldb18.PkduckDPEx;
import vldb18.PkduckDPExWIthLF;

public class PrefixSearch extends AbstractIndexBasedSearch {

	public static enum IndexChoice {
		Naive,
		Position,
	}

	protected boolean lf_query = true;
	protected boolean lf_text = true;
	protected final IndexChoice indexChoice;
	protected final GreedyValidator validator;
	protected TransSetBoundCalculatorInterface boundCalculator;
	protected PkduckDPEx pkduckdp = null;

	
	public PrefixSearch( double theta, boolean idxFilter_query, boolean idxFilter_text, boolean lf_query, boolean lf_text, IndexChoice indexChoice ) {
		super(theta, idxFilter_query, idxFilter_text);
		this.lf_query = lf_query;
		this.lf_text = lf_text;
		this.indexChoice = indexChoice;
		param.put("lf_query", Boolean.toString(lf_query));
		param.put("lf_text", Boolean.toString(lf_text));
		param.put("index_impl", indexChoice.toString());
		validator = new GreedyValidator();
	}
	
	@Override
	protected AbstractIndexBasedFilter buildSpecificIndex(Dataset dataset) {
		switch(indexChoice) {
		case Naive: return new NaiveIndexBasedFilter(dataset, theta, statContainer);
		case Position: return new PositionalIndexBasedFilter(dataset, theta, statContainer);
		default: throw new RuntimeException("Unknown index type: "+indexChoice);
		}
	}

	@Override
	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		statContainer.addCount(Stat.Num_QS_WindowSizeAll, Util.sumWindowSize(rec));
		IntSet expandedPrefix = getExpandedPrefix(query);
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		Log.log.debug("wRange=(%d,%d)", wRange.min, wRange.max);
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				if ( witer.getSetSize() > wRange.max ) break;
				if ( witer.getSetSize() < wRange.min ) continue;
				int w = window.size();
				statContainer.addCount(Stat.Num_QS_WindowSizeLF, w);
				IntSet wprefix = witer.getPrefix();
				if (Util.hasIntersection(wprefix, expandedPrefix)) {
					statContainer.addCount(Stat.Num_QS_WindowSizeVerified, w);
					statContainer.startWatch(Stat.Time_3_Validation);
					boolean isSim = verifyQuerySide(query, window);
					statContainer.stopWatch(Stat.Time_3_Validation);
					statContainer.increment(Stat.Num_QS_Verified);
					if ( isSim ) {
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						Log.log.debug("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
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
	
	protected IntRange getWindowSizeRangeQuerySide( Record query, RecordInterface rec ) {
		if (lf_query) {
			int lb = query.getTransSetLB();
			int min = Math.max(1, (int)Math.ceil(theta*lb));
			int max = (int)Math.min(1.0*query.getMaxTransLength()/theta, rec.size());
			return new IntRange(min, max);
		}
		else return new IntRange(1, rec.size());
	}
	
	protected boolean verifyQuerySide( Record query, Subrecord window ) {
		double sim = validator.simQuerySide(query, window.toRecord());
		if ( sim >= theta ) Log.log.debug("verifyQuerySide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getSuperRecord().getID(), ()->sim);
		return sim >= theta;
	}
	
	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
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
	
	protected double getModifiedTheta( Record query, Record rec ) {
		return theta * query.size() / (query.size() + 2*(rec.getMaxRhsSize()-1));
	}

	protected IntList getCandTokenList( Record query, Record rec, double theta ) {
		IntSet tokenSet = rec.getCandTokenSet();
		tokenSet.retainAll(Util.getPrefix(query, theta));
		return new IntArrayList( tokenSet.stream().sorted().iterator() );
	}

	protected void setBoundCalculator(Record rec, double modifiedTheta) {
		if ( lf_text ) {
			statContainer.startWatch("Time_TransSetBoundCalculatorMem");
			boundCalculator = new TransSetBoundCalculator(statContainer, rec, modifiedTheta);
			statContainer.stopWatch("Time_TransSetBoundCalculatorMem");
		}
	}
	
	protected void setPkduckDP(Record query, Record rec, double modifiedTheta) {
		if ( lf_text ) pkduckdp = new PkduckDPExWIthLF(query, rec, boundCalculator, modifiedTheta);
		else pkduckdp = new PkduckDPEx(query, rec, modifiedTheta);
	}
	
	protected boolean applyPrefixFiltering( Record query, Record rec ) {
		for ( int widx=0; widx<rec.size(); ++widx ) {
			if ( applyPrefixFilteringFrom(query, rec, widx) ) return true;
		}
		Log.log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, ...)  isInSigU=false", query.getID(), rec.getID());
		return false;
	}
	
	protected boolean applyPrefixFilteringFrom( Record query, Record rec, int widx ) {
		for ( int w=1; w<=rec.size()-widx; ++w ) {
			Log.log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, ...)  widx=%d/%d  w=%d/%d", query.getID(), rec.getID(), widx, rec.size()-1, w, rec.size() );
			if ( lf_text ) {
				LFOutput lfOutput = applyLengthFiltering(query, widx, w);
				if ( lfOutput == LFOutput.filtered_ignore ) continue;
				else if ( lfOutput == LFOutput.filtered_stop ) break;
			}
			statContainer.addCount(Stat.Num_TS_WindowSizeLF, w);
			if ( applyPrefixFilteringToWindow(query, rec, widx, w) ) return true;
		}
		return false;
	}

	protected boolean applyPrefixFilteringToWindow( Record query, Record rec, int widx, int w ) {
		Log.log.trace("PrefixSearch.applyPrefixFiltering(query.id=%d, rec.id=%d, ...)  widx=%d/%d  w=%d/%d", query.getID(), rec.getID(), widx, rec.size()-1, w, rec.size());
		boolean isInSigU = pkduckdp.isInSigU(widx, w);
		if ( isInSigU ) {
			statContainer.addCount(Stat.Num_TS_WindowSizeVerified, w);
			Subrecord window = new Subrecord(rec, widx, widx+w);
			statContainer.startWatch(Stat.Time_3_Validation);
			boolean isSim = verifyTextSide(query, window);
			statContainer.stopWatch(Stat.Time_3_Validation);
			statContainer.increment(Stat.Num_TS_Verified);
			if (isSim) {
				rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
				Log.log.debug("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->w, ()->widx);
				return true;
			}
		}
		return false;
	}
	
	protected boolean verifyTextSide( Record query, Subrecord window ) {
		double sim = validator.simTextSide(query, window.toRecord());
		if ( sim >= theta ) Log.log.debug("verifyTextSide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getSuperRecord().getID(), ()->sim);
		return sim >= theta;
	}

	protected enum LFOutput {
		filtered_ignore,
		filtered_stop,
		not_filtered
	}
	
	protected LFOutput applyLengthFiltering( Record query, int widx, int w ) {
		int ub = boundCalculator.getLFUB(widx, widx+w-1);
		int lb = boundCalculator.getLFLB(widx, widx+w-1);
		int lbMono = boundCalculator.getLFLBMono(widx, widx+w-1);
		int qSetSize = query.getDistinctTokenCount();
		if ( qSetSize < lbMono ) {
			statContainer.increment("Num_TS_LFByLBMono");
			return LFOutput.filtered_stop;
		}
		if ( qSetSize > ub ) {
			statContainer.increment("Num_TS_LFByUB");
			return LFOutput.filtered_ignore;
		}
		if ( qSetSize < lb ) {
			statContainer.increment("Num_TS_LFByLB");
			return LFOutput.filtered_ignore;
		}
		else return LFOutput.not_filtered;
	}
	
	@Override
	public String getName() {
		return "PrefixSearch";
	}

	@Override
	public String getVersion() {
		return "3.00";
	}
}

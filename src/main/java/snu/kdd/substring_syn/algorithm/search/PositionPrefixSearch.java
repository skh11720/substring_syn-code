package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithEndpoints;
import snu.kdd.substring_syn.data.record.RecordWithPos;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PositionPrefixSearch extends PrefixSearch {
	
	public PositionPrefixSearch( double theta, boolean bLF, boolean bPF, IndexChoice indexChoice ) {
		super(theta, bLF, bPF, indexChoice);
	}

	@Override
	protected void searchRecordQuerySide( Record query, Record rec ) {
//		Log.log.trace("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		int sidx = ((RecordWithEndpoints)rec).getStartPoint();
		IntList epList = ((RecordWithEndpoints)rec).getEndpoints();

//		Log.log.trace("wRange=(%d,%d)", ()->wRange.min, ()->wRange.max);
		for ( int eidx : epList ) {
			int w = eidx - sidx;

			if ( bLF ) {
				switch ( applyLengthFilterQuerySide(w, wRange) ) {
				case filtered_ignore: continue;
				case filtered_stop: break;
				default:
				}
				statContainer.addCount(Stat.Len_QS_LF, w);
			}
			Subrecord window = new Subrecord(rec, sidx, eidx);
			if ( bPF && isFilteredByPrefixFilteringQuerySide(window) ) continue;

			statContainer.addCount(Stat.Len_QS_PF, w); 
			statContainer.startWatch(Stat.Time_QS_Validation);
			boolean isSim = verifyQuerySide(query, window);
			statContainer.stopWatch(Stat.Time_QS_Validation);
			if ( isSim ) {
				rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
//					Log.log.trace("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
//					Log.log.trace("rsltFromQueryMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
				return;
			}
		}
	}

	protected boolean isFilteredByPrefixFilteringQuerySide( Subrecord window ) {
		IntCollection wprefix = Util.getPrefix(window, theta);
		return !Util.hasIntersection(wprefix, expandedPrefix);
	}
	

	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
//		Log.log.trace("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		
		if (bLF || bPF) {
			statContainer.startWatch("Time_TS_searchRecord.transLen");
			transLenCalculator = new TransLenCalculator(statContainer, rec, modifiedTheta);
			statContainer.stopWatch("Time_TS_searchRecord.transLen");
		}
		
		if (bPF) searchRecordTextSideWithPrefixFilter(query, rec);
		else searchRecordTextSideWithoutPrefixFilter(query, rec);;
	}
	
	@Override
	protected void searchRecordTextSideWithPrefixFilter( Record query, Record rec ) {
		double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		statContainer.startWatch("Time_TS_searchRecordPF.getCandTokenList");
		IntList candTokenList = getCandTokenList(query, rec, modifiedTheta);
		statContainer.stopWatch("Time_TS_searchRecordPF.getCandTokenList");
		int widx = ((RecordWithEndpoints)rec).getStartPoint();
		IntList eidxList = ((RecordWithEndpoints)rec).getEndpoints();
		PkduckDPExIncremental pkduckdp = new PkduckDPExIncrementalOpt(query, rec, modifiedTheta);
//		Log.log.trace("searchRecordTextSideWithPF(%d, %d)\tcandTokenList=%s", ()->query.getID(), ()->rec.getID(), ()->candTokenList);
		ObjectSet<IntPair> verifiedWindowSet = new ObjectOpenHashSet<>();
		
		for ( int target : candTokenList ) {
			statContainer.startWatch("Time_TS_searchRecordPF.setTarget");
			pkduckdp.setTarget(target);
			statContainer.stopWatch("Time_TS_searchRecordPF.setTarget");
			statContainer.startWatch("Time_TS_searchRecordPF.initPkduck");
			pkduckdp.init();
			statContainer.stopWatch("Time_TS_searchRecordPF.initPkduck");
			int j = 0;
			while ( j < eidxList.size() && eidxList.get(j) <= widx ) ++j;
			for ( int w=1; w<=rec.size()-widx; ++w ) {
//					Log.log.trace("target=%s (%d), widx=%d, w=%d", Record.tokenIndex.getToken(target), target, widx, w);
				if ( bLF ) {
//						Log.log.trace("lb=%d, query.size=%d", transLenCalculator.getLFLB(widx, widx+w-1), query.size());
					if ( transLenCalculator.getLFLB(widx, widx+w-1) > query.size() ) break;
					statContainer.addCount(Stat.Len_TS_LF, w);
				}
				statContainer.startWatch("Time_TS_searchRecordPF.pkduck");
				pkduckdp.compute(widx+1, w);
				statContainer.stopWatch("Time_TS_searchRecordPF.pkduck");

				if ( j >= eidxList.size() ) break;
				if ( eidxList.get(j) != widx+w ) continue;
				++j;
				
				if ( verifiedWindowSet.contains(new IntPair(widx, w)) ) continue;
				if ( pkduckdp.isInSigU(widx, w) ) {
					verifiedWindowSet.add(new IntPair(widx, w));
					statContainer.addCount(Stat.Len_TS_PF, w);
					Subrecord window = new Subrecord(rec, widx, widx+w);
					boolean isSim = verifyTextSideWrapper(query, window);
					if ( isSim ) {
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
//							Log.log.trace("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
//							Log.log.trace("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
						return;
					}
				}
			}
		}
	}
	
	@Override
	protected void searchRecordTextSideWithoutPrefixFilter( Record query, Record rec ) {
		int widx = ((RecordWithEndpoints)rec).getStartPoint();
		IntList eidxList = ((RecordWithEndpoints)rec).getEndpoints();
//		Log.log.trace("rec =\n%s", rec.toStringDetails());
//		Log.log.trace("prefixIdxList=%s", prefixIdxList);
//		Log.log.trace("suffixIdxList=%s", suffixIdxList);
		int j = 0;
		while ( j < eidxList.size() && eidxList.get(j) <= widx ) ++j;
		for ( int w=1; w<=rec.size()-widx; ++w ) {
			if ( j >= eidxList.size() ) break;
			if ( eidxList.get(j) != widx+w ) continue;
			++j;
//				Log.log.trace("searchRecordTextSideWithoutPrefixFilter\trec.id=%d, widx=%d, w=%d", rec.getID(), widx, w);
			if ( bLF ) {
//					Log.log.trace("searchRecordTextSideWithoutPrefixFilter.transLenCalculator\tLFLB=%d, query.size=%d", transLenCalculator.getLFLB(widx, widx+w-1), query.size());
				if ( transLenCalculator.getLFLB(widx, widx+w-1) > query.size() ) break;
				statContainer.addCount(Stat.Len_TS_LF, w);
			}
			Subrecord window = new Subrecord(rec, widx, widx+w);
			boolean isSim = verifyTextSideWrapper(query, window);
			if ( isSim ) {
				rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
//					Log.log.trace("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
//					Log.log.trace("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
				return;
			}
		}
	}
}

package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
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
		Log.log.debug("searchRecordFromQuery(%d, %d)", ()->query.getID(), ()->rec.getID());
		IntList posList = getCommonTokenPosList(rec);
		IntRange wRange = getWindowSizeRangeQuerySide(query, rec);
		Log.log.debug("wRange=(%d,%d)", wRange.min, wRange.max);
		for ( int i=0; i<posList.size(); ++i ) {
			int sidx = posList.get(i);
			for ( int j=i; j<posList.size(); ++j ) {
				int eidx = posList.get(j)+1; // exclusive
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
				statContainer.startWatch(Stat.Time_Validation);
				boolean isSim = verifyQuerySide(query, window);
				statContainer.stopWatch(Stat.Time_Validation);
				if ( isSim ) {
					rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
					Log.log.debug("rsltFromQuery.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
					Log.log.debug("rsltFromQueryMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
					return;
				}
			}
		}
	}

	protected IntList getCommonTokenPosList( Record rec ) {
		IntList posList = new IntArrayList();
		for ( int i=0; i<rec.size(); ++i ) {
			if ( queryCandTokenSet.contains(rec.getToken(i)) ) posList.add(i);
		}
		return posList;
	}

	protected boolean isFilteredByPrefixFilteringQuerySide( Subrecord window ) {
		IntCollection wprefix = Util.getPrefix(window, theta);
		return !Util.hasIntersection(wprefix, expandedPrefix);
	}
	

	@Override
	protected void searchRecordTextSide( Record query, Record rec ) {
		Log.log.debug("searchRecordFromText(%d, %d)", ()->query.getID(), ()->rec.getID());
		double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		
		if (bLF) {
			statContainer.startWatch("Time_TransSetBoundCalculator");
			transLenCalculator = new TransLenCalculator(statContainer, rec, modifiedTheta);
			statContainer.stopWatch("Time_TransSetBoundCalculator");
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
		IntList prefixIdxList = ((RecordWithPos)rec).getPrefixIdxList();
		IntList suffixIdxList = ((RecordWithPos)rec).getSuffixIdxList();
		PkduckDPExIncremental pkduckdp = new PkduckDPExIncremental(query, rec, modifiedTheta);
		Log.log.trace("searchRecordTextSideWithPF(%d, %d)\tcandTokenList=%s", query.getID(), rec.getID(), candTokenList);
		
		for ( int target : candTokenList ) {
			for ( int widx : prefixIdxList ) {
				statContainer.startWatch("Time_TS_searchRecordPF.initPkduck");
				pkduckdp.init();
				statContainer.stopWatch("Time_TS_searchRecordPF.initPkduck");
				int j = 0;
				while ( j < suffixIdxList.size() && suffixIdxList.get(j) < widx ) ++j;
				for ( int w=1; w<=rec.size()-widx; ++w ) {
//					Log.log.trace("target=%s (%d), widx=%d, w=%d", Record.tokenIndex.getToken(target), target, widx, w);
					if ( bLF ) {
//						Log.log.trace("lb=%d, query.size=%d", transLenCalculator.getLFLB(widx, widx+w-1), query.size());
						if ( transLenCalculator.getLFLB(widx, widx+w-1) > query.size() ) break;
						statContainer.addCount(Stat.Len_TS_LF, w);
					}
					statContainer.startWatch("Time_TS_Pkduck");
					pkduckdp.compute(target, widx+1, w);
					statContainer.stopWatch("Time_TS_Pkduck");

					if ( j >= suffixIdxList.size() ) break;
					if ( suffixIdxList.get(j)+1 != widx+w ) continue;
					++j;
					
					if ( pkduckdp.isInSigU(widx, w) ) {
						statContainer.addCount(Stat.Len_TS_PF, w);
						Subrecord window = new Subrecord(rec, widx, widx+w);
						boolean isSim = verifyTextSideWrapper(query, window);
						if ( isSim ) {
							rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
							Log.log.debug("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
							Log.log.debug("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
							return;
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void searchRecordTextSideWithoutPrefixFilter( Record query, Record rec ) {
		IntList prefixIdxList = ((RecordWithPos)rec).getPrefixIdxList();
		IntList suffixIdxList = ((RecordWithPos)rec).getSuffixIdxList();
		for ( int widx : prefixIdxList ) {
			int j = 0;
			while ( j < suffixIdxList.size() && suffixIdxList.get(j) < widx ) ++j;
			for ( int w=1; w<=rec.size()-widx; ++w ) {
				if ( j >= suffixIdxList.size() ) break;
				if ( suffixIdxList.get(j)+1 != widx+w ) continue;
				++j;
				if ( bLF ) {
					if ( transLenCalculator.getLFLB(widx, widx+w-1) > query.size() ) break;
					statContainer.addCount(Stat.Len_TS_LF, w);
				}
				Subrecord window = new Subrecord(rec, widx, widx+w);
				boolean isSim = verifyTextSideWrapper(query, window);
				if ( isSim ) {
					rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
					Log.log.debug("rsltFromText.add(%d, %d), w=%d, widx=%d", ()->query.getID(), ()->rec.getID(), ()->window.size(), ()->window.sidx);
					Log.log.debug("rsltFromTextMatch\t%s ||| %s", ()->query.toOriginalString(), ()->window.toOriginalString());
					return;
				}
			}
		}
	}
}

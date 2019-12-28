package snu.kdd.faerie;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSynSearch extends FaerieSearch {
	
	private FaerieSynIndex indexT = null;

	public FaerieSynSearch(double theta) {
		super(theta);
	}

	@Override
	protected final void prepareSearch( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_BuildIndex);
		index = new FaerieIndex(dataset.getIndexedList(), "FaerieSynSearch_index");
		indexT = new FaerieSynIndex(dataset.getIndexedList(), "FaerieSynSearch_indexT");
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.setStat(Stat.SpaceUsage_Index, diskSpaceUsage().toString());
	}

	@Override
	protected final void search( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_QS_Total);
		searchQuerySide(dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
		statContainer.startWatch(Stat.Time_TS_Total);
		searchTextSide(dataset);
		statContainer.stopWatch(Stat.Time_TS_Total);
	}

	protected final void searchQuerySide( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
//			if ( query.getID() != 0 ) return; else Log.log.trace("query_%d=%s", query.getID(), query.toOriginalString());
			for ( Record rec : dataset.getIndexedList() ) {
				for ( Record queryExp : Records.expands(query) ) {
					int minLen = (int)Math.ceil(queryExp.size()*theta);
					int maxLen = (int)Math.floor(queryExp.size()/theta);
//			Log.log.trace("minLen, maxLen = %d, %d", minLen, maxLen);
	//				if ( rec.getID() != 946 ) continue; else Log.log.trace("rec_%d=%s", rec.getID(), rec.toOriginalString());
					IntList posList = getPosList(queryExp, rec);
					statContainer.startWatch(Stat.Time_QS_Validation);
					boolean isSim = searchRecord(queryExp, rec, posList, minLen, maxLen, this::computeSimQuerySide);
					statContainer.stopWatch(Stat.Time_QS_Validation);
					if ( isSim ) {
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
		}
	}

	protected double computeSimQuerySide(Record query, Subrecord window) {
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return Util.jaccardM(query.getTokenList(), window.getTokenList());
	}

	protected final void searchTextSide( Dataset dataset ) {
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessAll();
			FaerieSynIndexEntry entry = indexT.getEntry(rec.getID());
			for ( Record query : dataset.getSearchedList() ) {
				IntSet tokenSet = query.getDistinctTokens();
//				if ( query.getID() != 0 ) return; else Log.log.trace("query_%d=%s", query.getID(), query.toOriginalString());
				int minLen = (int)Math.ceil(query.size()*theta);
				int maxLen = (int)Math.floor(query.size()/theta);
//				Log.log.trace("minLen, maxLen = %d, %d", minLen, maxLen);
				int i = 0;
				for ( Record recExp : Records.expands(rec) ) {
					Int2ObjectMap<IntList> invIndex = entry.invIndexList.get(i);
//					if ( rec.getID() != 946 ) continue; else Log.log.trace("rec_%d=%s", rec.getID(), rec.toOriginalString());
					IntList posList = getPosList(tokenSet, invIndex);
					statContainer.startWatch(Stat.Time_TS_Validation);
					boolean isSim = searchRecord(query, recExp, posList, minLen, maxLen, this::computeSimTextSide);
					statContainer.stopWatch(Stat.Time_TS_Validation);
					if ( isSim ) {
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
					i += 1;
				}
			}
		}
	}

	protected double computeSimTextSide(Record query, Subrecord window) {
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		return Util.jaccardM(query.getTokenList(), window.getTokenList());
	}

	protected BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage().add(indexT.diskSpaceUsage());
	}

	@Override
	public String getName() {
		return "FaerieSynSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

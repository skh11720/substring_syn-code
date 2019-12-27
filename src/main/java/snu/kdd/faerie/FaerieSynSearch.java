package snu.kdd.faerie;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public class FaerieSynSearch extends FaerieSearch {
	
	private FaerieSynIndex indexT = null;

	public FaerieSynSearch(double theta) {
		super(theta);
	}

	@Override
	protected final void prepareSearch( Dataset dataset ) {
		super.prepareSearch(dataset);
		indexT = new FaerieSynIndex(dataset.getIndexedList());
	}

	@Override
	protected final void search( Dataset dataset ) {
		searchQuerySide(dataset);
		searchTextSide(dataset);;
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
					boolean isSim = searchRecord(queryExp, rec, posList, minLen, maxLen);
					if ( isSim ) {
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
		}
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
					boolean isSim = searchRecord(query, recExp, posList, minLen, maxLen);
					if ( isSim ) {
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
					i += 1;
				}
			}
		}
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

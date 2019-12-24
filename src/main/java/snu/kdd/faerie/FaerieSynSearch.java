package snu.kdd.faerie;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public class FaerieSynSearch extends FaerieSearch {

	public FaerieSynSearch(double theta) {
		super(theta);
	}

	@Override
	protected void search( Dataset dataset ) {
		searchQuerySide(dataset);
		searchTextSide(dataset);;
	}

	protected void searchQuerySide( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
//			if ( query.getID() != 0 ) return; else Log.log.trace("query_%d=%s", query.getID(), query.toOriginalString());
			for ( Record rec : dataset.getIndexedList() ) {
				for ( Record queryExp : Records.expands(query) ) {
					int minLen = (int)Math.ceil(queryExp.size()*theta);
					int maxLen = (int)Math.floor(queryExp.size()/theta);
//			Log.log.trace("minLen, maxLen = %d, %d", minLen, maxLen);
	//				if ( rec.getID() != 946 ) continue; else Log.log.trace("rec_%d=%s", rec.getID(), rec.toOriginalString());
					IntList posList = getPosListQuerySide(queryExp, rec);
					boolean isSim = searchRecord(queryExp, rec, posList, minLen, maxLen);
					if ( isSim ) {
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
		}
	}

	protected final IntList getPosListQuerySide( Record queryExp, Record rec ) {
		return getPosList(queryExp, rec);
	}

	protected void searchTextSide( Dataset dataset ) {
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessAll();
			for ( Record query : dataset.getSearchedList() ) {
	//			if ( query.getID() != 0 ) return; else Log.log.trace("query_%d=%s", query.getID(), query.toOriginalString());
				int minLen = (int)Math.ceil(query.size()*theta);
				int maxLen = (int)Math.floor(query.size()/theta);
	//			Log.log.trace("minLen, maxLen = %d, %d", minLen, maxLen);
				for ( Record recExp : Records.expands(rec) ) {
	//				if ( rec.getID() != 946 ) continue; else Log.log.trace("rec_%d=%s", rec.getID(), rec.toOriginalString());
					IntList posList = getPosListTextSide(query, recExp);
					boolean isSim = searchRecord(query, recExp, posList, minLen, maxLen);
					if ( isSim ) {
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
		}
	}

	protected final IntList getPosListTextSide( Record query, Record recExp ) {
		IntList posList = new IntArrayList();
		IntSet tokens = new IntOpenHashSet(query.getTokens());
		for ( int i=0; i<recExp.size(); ++i ) {
			int token = recExp.getToken(i);
			if ( tokens.contains(token) ) posList.add(i);
		}
		return posList;
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

package snu.kdd.faerie;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSearch extends AbstractSearch {
	
	private FaerieIndex index = null;

	public FaerieSearch(double theta) {
		super(theta);
	}
	
	@Override
	public void run(Dataset dataset) {
		statContainer.setAlgorithm(this);
		statContainer.mergeStatContainer(dataset.statContainer);
		statContainer.startWatch(Stat.Time_Total);
		prepareSearch(dataset);
		search(dataset);
		statContainer.stopWatch(Stat.Time_Total);
		putResultIntoStat();
		statContainer.finalizeAndOutput();
		outputResult(dataset);
	}

	@Override
	protected void prepareSearch( Dataset dataset ) {
		index = new FaerieIndex(dataset.getIndexedList());
	}
	
	protected final void search( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
//			if ( query.getID() != 0 ) return; else Log.log.trace("query_%d=%s", query.getID(), query.toOriginalString());
			int minLen = (int)Math.ceil(query.size()*theta);
			int maxLen = (int)Math.floor(query.size()/theta);
//			Log.log.trace("minLen, maxLen = %d, %d", minLen, maxLen);
			for ( Record rec : dataset.getIndexedList() ) {
//				if ( rec.getID() != 946 ) continue; else Log.log.trace("rec_%d=%s", rec.getID(), rec.toOriginalString());
				searchRecord(query, rec, minLen, maxLen);
			}
		}
	}
	
	protected void searchRecord( Record query, Record rec, int minLen, int maxLen ) {
		IntList posList = getPosList(query, rec);
//				Log.log.trace("rec.id=%d, posList=%s", ()->rec.getID(), ()->posList);
		for ( int i=0; i<posList.size()-minLen+1; ++i ) {
			for ( int j = i+minLen-1; j < posList.size(); j++ ) {
				if ( posList.get(j)-posList.getInt(i)+1 > maxLen ) break;
				int sidx = posList.getInt(i);
				int eidx = posList.getInt(j)+1;
				Record window = rec.getSubrecord(sidx, eidx);
//						Log.log.trace("window = rec%d[%d:%d] = %s", rec.getID(), sidx, eidx, window.toOriginalString());
				double sim = Util.jaccardM(query.getTokenList(), window.getTokenList());
				if ( sim >= theta ) {
//							Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
					rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
					return;
				}
			}
		}
	}
	
	protected final IntList getPosList( Record query, Record rec ) {
		FaerieIndexEntry entry = index.entryList.get(rec.getID());
		ObjectList<IntList> posLists = new ObjectArrayList<>();
		for ( int token : query.getTokens() ) {
			posLists.add(entry.tok2posListMap.get(token));
		}
		return Util.mergeSortedIntLists(posLists);
	}
	
//	private class SimilarityCalculator {
//		final Int2IntOpenHashMap counter;
//		int num = 0, den = 0;
//
//		public SimilarityCalculator( Record query ) {
//			counter = new Int2IntOpenHashMap();
//			for ( int token : query.getTokens() ) counter.addTo(token, 1);
//			den = query.size();
//		}
//		
//		public void add( int token ) {
//			if ( counter.containsKey(token) && counter.get(token) > 0 ) {
//				num += 1;
//				counter.addTo(token, -1);
//			}
//			else den += 1;
//		}
//		
//		public double compute() {
//			return num/den;
//		}
//	}

	@Override
	protected void searchRecordQuerySide(Record query, Record rec) {
	}

	@Override
	protected void searchRecordTextSide(Record query, Record rec) {
	}

	@Override
	public String getName() {
		return "FaerieSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

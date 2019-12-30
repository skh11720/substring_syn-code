package snu.kdd.faerie;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSearch extends AbstractSearch {
	
	protected FaerieIndexInterface index = null;
	protected final boolean isDiskBased;

	public FaerieSearch(double theta, boolean isDiskBased) {
		super(theta);
		this.isDiskBased = isDiskBased;
		param.put("isDiskBased", Boolean.toString(isDiskBased));
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
		statContainer.startWatch(Stat.Time_BuildIndex);
		if (isDiskBased) index = new FaerieMemBasedIndex(dataset.getIndexedList());
		else index = new FaerieDiskBasedIndex(dataset.getIndexedList());
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.setStat(Stat.SpaceUsage_Index, diskSpaceUsage().toString());
	}
	
	protected void search( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
//			if ( query.getID() != 24 ) continue; else Log.log.trace("query_%d=%s", query.getID(), query.toOriginalString());
			int minLen = (int)Math.ceil(query.size()*theta);
			int maxLen = (int)Math.floor(query.size()/theta);
//			Log.log.trace("minLen, maxLen = %d, %d", minLen, maxLen);
			for ( Record rec : dataset.getIndexedList() ) {
//				if ( rec.getID() != 9185 ) continue; else Log.log.trace("rec_%d=%s", rec.getID(), rec.toOriginalString());
				IntList posList = getPosList(query, rec);
				boolean isSim = searchRecord(query, rec, posList, minLen, maxLen, this::computeSim);
				if ( isSim ) rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
			}
		}
	}
	
	protected final IntList getPosList( Record query, Record rec ) {
		FaerieIndexEntry entry = index.getEntry(rec.getID());
		return getPosList( query.getDistinctTokens(), entry.tok2posListMap);
	}
	
	protected final IntList getPosList( IntSet tokenSet, Int2ObjectMap<IntList> invIndex ) {
		ObjectList<IntList> posLists = new ObjectArrayList<>();
		for ( int token : tokenSet ) posLists.add(invIndex.get(token));
		return Util.mergeSortedIntLists(posLists);
	}
	
	@FunctionalInterface
	protected interface SimCalculator {
		double run(Record query, Subrecord window);
	}

	protected boolean searchRecord( Record query, Record rec, IntList posList, int minLen, int maxLen, SimCalculator verifier ) {
//		Log.log.trace("rec.id=%d, posList=%s", ()->rec.getID(), ()->posList);
		int i = 0;
		while ( i < posList.size()-minLen+1 ) {
//			Log.log.trace("while: i=%d, limit=%d", i, posList.size()-minLen+1);
			int j = i+minLen-1;
			if ( posList.get(j)-posList.getInt(i)+1 <= maxLen ) {
				int mid = binarySpan(posList, i, j, maxLen);
				for ( int k=j; k<=mid; k++ ) {
					int sidx = posList.getInt(i);
					int eidx = posList.getInt(k)+1;
					Subrecord window = new Subrecord(rec, sidx, eidx);
//					Log.log.trace("window = rec%d[%d:%d] = %s", rec.getID(), sidx, eidx, window.toOriginalString());
					double sim = verifier.run(query, window);
					if ( sim >= theta ) return true;
				}
				i += 1;
			}
			else i = binaryShift(posList, i, j, maxLen);
		}
		return false;
	}
	
	private double computeSim(Record query, Subrecord window) {
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return Util.jaccardM(query.getTokenList(), window.getTokenList());
	}
	
	protected final int binarySpan( IntList posList, int i, int j, int maxLen ) {
		int lower = j, upper = Math.min(i+maxLen-1, posList.size()-1);
		while ( lower <= upper ) {
			int mid = (upper+lower+1)/2;
			if ( posList.getInt(mid)-posList.getInt(i)+1 > maxLen ) upper = mid-1;
			else lower = mid+1;
		}
		return upper;
	}
	
	protected final int binaryShift( IntList posList, int i, int j, int maxLen ) {
		int lower = i, upper = j;
		while ( lower <= upper ) {
			int mid = (upper+lower+1)/2;
			if ( posList.getInt(j)+(mid-i)-posList.getInt(mid)+1 > maxLen ) lower = mid+1;
			else upper = mid-1;
		}
//		Log.log.trace("binaryShift(i=%d, j=%d): %d", i, j, lower);
		return lower;
	}
	
	protected BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage(); 
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

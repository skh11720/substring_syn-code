package snu.kdd.faerie;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSearch extends AbstractSearch {
	
	protected FaerieIndexInterface index = null;
	protected final boolean isDiskBased;
	
	protected int minLenQS, maxLenTS;

	public FaerieSearch(double theta, boolean isDiskBased) {
		super(theta);
		this.isDiskBased = isDiskBased;
		param.put("isDiskBased", Boolean.toString(isDiskBased));
	}
	
	@Override
	protected void prepareSearch( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_BuildIndex);
		if (isDiskBased) index = new FaerieMemBasedIndex(dataset.getIndexedList());
		else index = new FaerieDiskBasedIndex(dataset.getIndexedList());
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.setStat(Stat.Space_Index, diskSpaceUsage().toString());
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
		minLenQS = (int)Math.ceil(query.size()*theta);
		maxLenTS = (int)Math.floor(query.size()/theta);
	}
	
	@Override
	protected void searchRecordQuerySide(Record query, RecordInterface rec) {
		IntList posList = getPosList(query, rec);
		boolean isSim = searchRecord(query, rec, posList, minLenQS, maxLenTS, this::computeSim);
		if ( isSim ) addResultQuerySide(query, rec);
	}

	@Override
	protected void searchRecordTextSide(Record query, TransformableRecordInterface rec) {
	}

	protected final IntList getPosList( Record query, RecordInterface rec ) {
		FaerieIndexEntry entry = index.getEntry(rec.getIdx());
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

	protected boolean searchRecord( Record query, RecordInterface rec, IntList posList, int minLen, int maxLen, SimCalculator verifier ) {
		int i = 0;
		while ( i < posList.size()-minLen+1 ) {
			int j = i+minLen-1;
			if ( posList.get(j)-posList.getInt(i)+1 <= maxLen ) {
				int mid = binarySpan(posList, i, j, maxLen);
				for ( int k=j; k<=mid; k++ ) {
					int sidx = posList.getInt(i);
					int eidx = posList.getInt(k)+1;
					Subrecord window = new Subrecord(rec, sidx, eidx);
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
		return lower;
	}
	
	protected BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage(); 
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

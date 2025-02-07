package snu.kdd.faerie;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSynSearch extends FaerieSearch {
	
	private FaerieSynIndexInterface indexT = null;
	private IntSet queryTokenSet = null;

	public FaerieSynSearch(double theta, boolean isDiskBased) {
		super(theta, isDiskBased);
	}

	@Override
	protected final void prepareSearch( Dataset dataset ) {
		Log.log.trace("FaerieSynSearch.prepareSearch");
		Log.log.trace("isDiskBased=%s", ()->isDiskBased);
		statContainer.startWatch(Stat.Time_BuildIndex);
		if (isDiskBased) {
			index = new FaerieDiskBasedIndex(dataset.getIndexedList(), "FaerieSynDiskBasedSearch_index");
			indexT = new FaerieSynDiskBasedIndex(dataset.getIndexedList(), "FaerieSynDiskBasedSearch_indexT");
		}
		else {
			index = new FaerieMemBasedIndex(dataset.getIndexedList(), "FaerieSynMemBasedSearch_index");
			indexT = new FaerieSynMemBasedIndex(dataset.getIndexedList());
		}
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.setStat(Stat.Space_Index, diskSpaceUsage().toString());
	}
	
	@Override
	protected void prepareSearchGivenQuery(Record query) {
		super.prepareSearchGivenQuery(query);
		query.preprocessAll();
		queryTokenSet = query.getDistinctTokens();
	}
	
	@Override
	protected void searchRecordQuerySide(Record query, RecordInterface rec) {
		FaerieIndexEntry entry = index.getEntry(rec.getIdx());
		for ( Record queryExp : Records.expands(query) ) {
			int minLen = (int)Math.ceil(queryExp.size()*theta);
			int maxLen = (int)Math.floor(queryExp.size()/theta);
			IntList posList = getPosList( queryExp.getDistinctTokens(), entry.tok2posListMap);
			statContainer.startWatch(Stat.Time_QS_Validation);
			boolean isSim = searchRecord(queryExp, rec, posList, minLen, maxLen, this::computeSimQuerySide);
			statContainer.stopWatch(Stat.Time_QS_Validation);
			if ( isSim ) {
				addResultQuerySide(query, rec);
				break;
			}
		}
	}

	protected double computeSimQuerySide(Record query, Subrecord window) {
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		return Util.jaccardM(query.getTokenList(), window.getTokenList());
	}
	
	@Override
	protected void searchRecordTextSide(Record query, TransformableRecordInterface rec) {
		for ( FaerieSynIndexEntry entry : indexT.getRecordEntries(rec.getIdx()) ) {
			IntList posList = getPosList(queryTokenSet, entry.invIndex);
			statContainer.startWatch(Stat.Time_TS_Validation);
			boolean isSim = searchRecord(query, new Record(entry.recExpTokenArr), posList, minLenQS, maxLenTS, this::computeSimTextSide);
			statContainer.stopWatch(Stat.Time_TS_Validation);
			if ( isSim ) {
				addResultTextSide(query, rec);
				break;
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
		return "1.02";
	}
}

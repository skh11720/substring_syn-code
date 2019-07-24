package snu.kdd.substring_syn.algorithm.index;

import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.index.PositionalInvertedIndex.IndexEntry;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;

public class PositionalIndexBasedFilter extends AbstractIndexBasedFilter {

	protected final PositionalInvertedIndex index;
    protected final boolean useCountFilter = true;
	
	public PositionalIndexBasedFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(theta, statContainer);
		index = new PositionalInvertedIndex(dataset);
	}
	
	@Override
	public ObjectSet<RecordInterface> querySideFilter( Record query ) {
		Log.log.debug("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
		statContainer.startWatch("Time_QS_PositionFilter");
		ObjectSet<RecordInterface> candRecordSet = new ObjectOpenHashSet<>();
		int minCount = (int)Math.ceil(theta*query.getTransSetLB());
		Log.log.trace("query.size()=%d, query.getTransSetLB()=%d", ()->query.size(), ()->query.getTransSetLB());
		Log.log.trace("minCount=%d", ()->minCount);
		Object2ObjectMap<Record, IntList> rec2idxListMap = getCommonTokenIdxLists(query);
		for ( Entry<Record, IntList> entry : rec2idxListMap.entrySet() ) {
			Record rec = entry.getKey();
			IntList idxList = entry.getValue();
			idxList.sort(Integer::compare);
			Log.log.trace("idxList=%s", ()->idxList);
			Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
			if ( useCountFilter && idxList.size() < minCount ) continue;
			ObjectList<RecordInterface> splitList =  pruneSingleRecord(query, rec, idxList, minCount);
			Log.log.trace("splitList=%s", ()->strSplitList(splitList));
			candRecordSet.addAll(splitList);
		}
		statContainer.stopWatch("Time_QS_PositionFilter");
		statContainer.addCount("Num_QS_PositionFilter", candRecordSet.size());
		return candRecordSet;
	}

	private Object2ObjectMap<Record, IntList> getCommonTokenIdxLists( Record query ) {
		Object2ObjectMap<Record, IntList> rec2idxListMap = new Object2ObjectOpenHashMap<Record, IntList>();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<IndexEntry> invList = index.getInvList(token);
			if ( invList == null ) continue; 
			for ( IndexEntry e : invList ) {
				Record rec = e.rec;
				int pos = e.pos;
				if ( !rec2idxListMap.containsKey(rec) ) rec2idxListMap.put(rec, new IntArrayList());
				rec2idxListMap.get(rec).add(pos);
			}
		}
		return rec2idxListMap;
	}
	
	private ObjectList<RecordInterface> pruneSingleRecord( Record query, Record rec, IntList idxList, int minCount ) {
		ObjectList<IntRange> segmentList = findSegments(rec, idxList, theta);
		Log.log.trace("segmentList=%s", ()->segmentList);
		ObjectList<RecordInterface> splitList = splitRecord(rec, segmentList, idxList, minCount );
		return splitList;
	}

	private ObjectList<IntRange> findSegments( Record rec, IntList idxList, double theta ) {
		int m = idxList.size();
		ObjectList<IntRange> rangeList = new ObjectArrayList<>();
		for ( int i=0; i<m-1; ++i ) {
			int sidx = idxList.get(i);
			IntSet numSet = new IntOpenHashSet(rec.getToken(sidx));
			IntSet denumSet = new IntOpenHashSet(rec.getToken(sidx));
			int eidx0 = sidx;
			for ( int j=i; j<m; ++j ) {
				int eidx1 = idxList.get(j);
				numSet.add(rec.getToken(eidx1));
				denumSet.addAll(rec.getTokenList().subList(eidx0+1, eidx1+1));
				double score = (double)numSet.size()/denumSet.size();
				Log.log.trace("sidx=%d, eidx1=%d, score=%.3f, theta=%.3f", ()->sidx, ()->eidx1, ()->score, ()->theta);
				if ( score >= theta ) {
					if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx1;
					else rangeList.add(new IntRange(sidx, eidx1));
					Log.log.trace("range=%s", ()->rangeList.get(rangeList.size()-1));
				}
				eidx0 = eidx1;
			}
		}
		Log.log.trace("rangeList=%s", ()->rangeList);
		if ( rangeList.size() == 0 ) return null;
		
		// merge
		ObjectList<IntRange> mergedRangeList = new ObjectArrayList<>();
		IntRange mergedRange = rangeList.get(0);
		for ( int i=1; i<rangeList.size(); ++i ) {
			IntRange thisRange = rangeList.get(i);
			if ( mergedRange.min == thisRange.min ) mergedRange.max = thisRange.max;
			else {
				if ( thisRange.min <= mergedRange.max ) mergedRange.max = Math.max(mergedRange.max, thisRange.max);
				else {
					mergedRangeList.add(mergedRange);
					mergedRange = thisRange;
				}
			}
		}
		mergedRangeList.add(mergedRange);
		return mergedRangeList;
	}
	
	private ObjectList<RecordInterface> splitRecord( Record rec, ObjectList<IntRange> segmentList, IntList idxList, int minCount ) {
		ObjectList<RecordInterface> splitList = new ObjectArrayList<>();
		if ( segmentList != null ) {
			for ( IntRange range : segmentList ) {
				int count = 0;
				for ( int idx : idxList ) {
					if ( range.min <= idx && idx <= range.max ) ++count;
				}
				if ( count >= minCount ) splitList.add(new Subrecord(rec, range.min, range.max+1));
			}
		}
		return splitList;
	}
	
	@Deprecated
	private double[] computeSplitScore( Record rec, IntList idxList ) {
		int m = idxList.size();
		double[] splitScoreArr = new double[m-1];
		for ( int i=0; i<m-1; ++i ) {
			int sidx = idxList.get(i);
			IntSet numSet = new IntOpenHashSet(rec.getToken(sidx));
			IntSet denumSet = new IntOpenHashSet(rec.getToken(sidx));
			int eidx0 = sidx;
			for ( int j=i+1; j<m; ++j ) {
				int eidx1 = idxList.get(j);
				numSet.add(rec.getToken(eidx1));
				denumSet.addAll(rec.getTokenList().subList(eidx0+1, eidx1+1));
				for ( int k=i; k<j; ++k ) splitScoreArr[k] = Math.max(splitScoreArr[k], (double)numSet.size()/denumSet.size());
				eidx0 = eidx1;
			}
		}
		return splitScoreArr;
	}
	
	@Deprecated
	private ObjectList<RecordInterface> splitRecordOld( Record rec, IntList idxList, double[] splitScoreArr, double theta, int minCount ) {
		ObjectList<RecordInterface> segmentList = new ObjectArrayList<>();
		int sidx = idxList.get(0);
		int eidx = -1;
		int count = 1;
		for ( int i=0; i<splitScoreArr.length; ++i ) {
			eidx = idxList.get(i+1);
			++count;
			if ( splitScoreArr[i] < theta ) {
				if ( !useCountFilter || count >= minCount ) segmentList.add(new Subrecord(rec, sidx, eidx+1));
				sidx = idxList.get(i+1);
				count = 1;
			}
		}
		if ( !useCountFilter || count >= minCount ) segmentList.add(new Subrecord(rec, sidx, eidx+1));
		return segmentList;
	}
	
	@Override
	public ObjectSet<Record> textSideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.size());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		for ( int token : query.getTokens() ) {
			ObjectList<IndexEntry> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( IndexEntry e : invList ) counter.addTo(e.rec, 1);
			}
			ObjectList<IndexEntry> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) {
				for ( IndexEntry e : transInvList ) counter.addTo(e.rec, 1);
			}
		}

		statContainer.startWatch("Time_TS_IndexCountFilter");
		ObjectSet<Record> candRecordSet = pruneRecordsByCount(counter, minCount);
		statContainer.stopWatch("Time_TS_IndexCountFilter");
		statContainer.addCount("Num_TS_IndexCountFilter", candRecordSet.size());
		return candRecordSet;
	}
	
	private ObjectSet<Record> pruneRecordsByCount( Object2IntMap<Record> counter, int minCount ) {
		ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
		for ( Object2IntMap.Entry<Record> entry : counter.object2IntEntrySet() ) {
			Record rec = entry.getKey();
			int count = entry.getIntValue();
			if ( !useCountFilter || count >= minCount ) candRecordSet.add(rec);
		}
		return candRecordSet;
	}
	
	private String strSplitList( ObjectList<RecordInterface> segmentList ) {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<segmentList.size(); ++i ) {
			if ( i > 0 ) strbld.append(", ");
			Subrecord subrec = (Subrecord)segmentList.get(i);
			strbld.append(String.format("(%d,%d)", subrec.sidx, subrec.eidx));
		}
		strbld.append("]");
		return strbld.toString();
	}
}

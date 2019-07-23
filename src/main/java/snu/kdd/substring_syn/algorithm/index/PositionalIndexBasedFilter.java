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
import snu.kdd.substring_syn.utils.StatContainer;

public class PositionalIndexBasedFilter extends AbstractIndexBasedFilter {

	protected final PositionalInvertedIndex index;
    protected final boolean useCountFilter = true;
	
	public PositionalIndexBasedFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(theta, statContainer);
		index = new PositionalInvertedIndex(dataset);
	}
	
	public ObjectSet<RecordInterface> querySideFilter( Record query ) {
		statContainer.startWatch("Time_QS_PositionFilter");
		ObjectSet<RecordInterface> candRecordSet = new ObjectOpenHashSet<>();
		int minCount = (int)Math.ceil(theta*query.size());
		Object2ObjectMap<Record, IntList> rec2idxListMap = getCommonTokenIdxLists(query);
		for ( Entry<Record, IntList> entry : rec2idxListMap.entrySet() ) {
			Record rec = entry.getKey();
			IntList idxList = entry.getValue();
			if ( useCountFilter && idxList.size() < minCount ) continue;
			ObjectList<RecordInterface> segmentList =  pruneSingleRecord(query, rec, idxList, minCount);
			candRecordSet.addAll(segmentList);
		}
		statContainer.stopWatch("Time_QS_PositionFilter");
		statContainer.addCount("Num_QS_PositionFilter", candRecordSet.size());
		return candRecordSet;
	}
	
	public ObjectSet<Record> textSideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.size());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		for ( int token : query.getTokens() ) {
			ObjectList<IndexEntry> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( IndexEntry e : invList ) counter.addTo(e.rec, 1);
			}
			ObjectList<Record> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) {
				for ( Record rec : transInvList ) counter.addTo(rec, 1);
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
		double[] splitScoreArr = computeSplitScore(rec, idxList);
		ObjectList<RecordInterface> segmentList = splitRecord(rec, idxList, splitScoreArr, theta, minCount);
		return segmentList;
	}
	
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
				splitScoreArr[j-1] = Math.max(splitScoreArr[j-1], (double)numSet.size()/denumSet.size());
				eidx0 = eidx1;
			}
		}
		return splitScoreArr;
	}
	
	private ObjectList<RecordInterface> splitRecord( Record rec, IntList idxList, double[] splitScoreArr, double theta, int minCount ) {
		ObjectList<RecordInterface> segmentList = new ObjectArrayList<>();
		int sidx = idxList.get(0);
		int eidx = -1;
		int count = 1;
		for ( int i=0; i<splitScoreArr.length; ++i ) {
			if ( splitScoreArr[i] < theta ) {
				eidx = idxList.get(i);
				if ( !useCountFilter || count >= minCount ) segmentList.add(new Subrecord(rec, sidx, eidx+1));
				sidx = idxList.get(i+1);
				count = 1;
			}
			++count;
		}
		return segmentList;
	}

	private void visualizeCandRecords( IntSet candTokenSet, ObjectSet<Record> candRecordList, Object2IntOpenHashMap<Record> counter ) {
		for ( Record rec : candRecordList ) {
			int[] tokens = rec.getTokenArray();
			StringBuilder strbld = new StringBuilder();
			for ( int token : tokens ) {
				if ( candTokenSet.contains(token) ) strbld.append("O");
				else strbld.append('-');
			}
			System.out.println(counter.getInt(rec)+"\t"+strbld.toString());
		}
	}
}

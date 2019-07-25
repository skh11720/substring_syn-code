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
import snu.kdd.substring_syn.algorithm.index.PositionalInvertedIndex.InvListEntry;
import snu.kdd.substring_syn.algorithm.index.PositionalInvertedIndex.TransInvListEntry;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
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
		QuerySideFilter filter = new QuerySideFilter();
		return filter.run(query);
	}
	
	private class QuerySideFilter {
		
		public ObjectSet<RecordInterface> run( Record query ) {
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
				ObjectList<RecordInterface> segmentList =  pruneSingleRecord(query, rec, idxList, minCount);
				Log.log.trace("segmentList=%s", ()->strSegmentList(segmentList));
				candRecordSet.addAll(segmentList);
			}
			statContainer.stopWatch("Time_QS_PositionFilter");
			statContainer.addCount("Num_QS_PositionFilter", candRecordSet.size());
			return candRecordSet;
		}
		
		private Object2ObjectMap<Record, IntList> getCommonTokenIdxLists( Record query ) {
			Object2ObjectMap<Record, IntList> rec2idxListMap = new Object2ObjectOpenHashMap<Record, IntList>();
			IntSet candTokenSet = query.getCandTokenSet();
			for ( int token : candTokenSet ) {
				ObjectList<InvListEntry> invList = index.getInvList(token);
				if ( invList == null ) continue; 
				for ( InvListEntry e : invList ) {
					Record rec = e.rec;
					int pos = e.pos;
					if ( !rec2idxListMap.containsKey(rec) ) rec2idxListMap.put(rec, new IntArrayList());
					rec2idxListMap.get(rec).add(pos);
				}
			}
			return rec2idxListMap;
		}
		
		private ObjectList<RecordInterface> pruneSingleRecord( Record query, Record rec, IntList idxList, int minCount ) {
			ObjectList<IntRange> segmentRangeList = findSegments(query, rec, idxList, theta);
			Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
			ObjectList<RecordInterface> segmentList = splitRecord(rec, segmentRangeList, idxList, minCount );
			return segmentList;
		}

		private ObjectList<IntRange> findSegments( Record query, Record rec, IntList idxList, double theta ) {
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
					double score = (double)numSet.size()/Math.max(query.getTransSetLB(), denumSet.size());
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
		
		private ObjectList<RecordInterface> splitRecord( Record rec, ObjectList<IntRange> segmentRangeList, IntList idxList, int minCount ) {
			ObjectList<RecordInterface> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( IntRange range : segmentRangeList ) {
					int count = 0;
					for ( int idx : idxList ) {
						if ( range.min <= idx && idx <= range.max ) ++count;
					}
					if ( count >= minCount ) segmentList.add(new Subrecord(rec, range.min, range.max+1));
				}
			}
			return segmentList;
		}
	}
	
	@Override
	public ObjectSet<Record> textSideFilter( Record query ) {
		TextSideFilter filter = new TextSideFilter();
		return filter.run(query);
	}
	
	private class TextSideFilter {
		
		public ObjectSet<Record> run( Record query ) {
			statContainer.startWatch("Time_TS_PositionFilter");
//			ObjectSet<RecordInterface> candRecordSet = new ObjectOpenHashSet<>();
			int minCount = (int)Math.ceil(theta*query.size());
			Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
			for ( int token : query.getTokens() ) {
				ObjectList<InvListEntry> invList = index.getInvList(token);
				if ( invList != null ) {
					for ( InvListEntry e : invList ) counter.addTo(e.rec, 1);
				}
				ObjectList<TransInvListEntry> transInvList = index.getTransInvList(token);
				if ( transInvList != null ) {
					for ( TransInvListEntry e : transInvList ) counter.addTo(e.rec, 1);
				}
			}

			ObjectSet<Record> candRecordSet = pruneRecordsByCount(counter, minCount);
			statContainer.stopWatch("Time_TS_PositionFilter");
			statContainer.addCount("Num_TS_PositionFilter", candRecordSet.size());
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

		private class TokenPosPair {
			int token;
			int pos;
			
			public TokenPosPair( int token, int pos ) {
				this.token = token;
				this.pos = pos;
			}
		}
	}
	
	private String strSegmentList( ObjectList<RecordInterface> segmentRangeList ) {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<segmentRangeList.size(); ++i ) {
			if ( i > 0 ) strbld.append(", ");
			Subrecord subrec = (Subrecord)segmentRangeList.get(i);
			strbld.append(String.format("(%d,%d)", subrec.sidx, subrec.eidx));
		}
		strbld.append("]");
		return strbld.toString();
	}
}

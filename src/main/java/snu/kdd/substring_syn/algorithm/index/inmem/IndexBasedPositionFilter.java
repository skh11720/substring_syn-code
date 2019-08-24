package snu.kdd.substring_syn.algorithm.index.inmem;

import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithPos;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class IndexBasedPositionFilter extends AbstractIndexBasedFilter implements PositionalIndexInterface {

	protected final PositionalInvertedIndex index;
	
	public IndexBasedPositionFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(dataset, theta, statContainer);
		index = new PositionalInvertedIndex(dataset);
	}

	@Override
	public long invListSize() {
		long size = 0;
		for ( ObjectList<InvListEntry> list : index.invList.values() ) size += list.size();
		return size;
	}
	
	@Override
	public long transInvListSize() {
		long size = 0;
		for ( ObjectList<TransInvListEntry> list : index.transInvList.values() ) size += list.size();
		return size;
	}
	
	@Override
	public ObjectSet<Record> querySideFilter( Record query ) {
		QuerySideFilter filter = new QuerySideFilter();
		return filter.run(query);
	}
	
	private class QuerySideFilter {
		
		public ObjectSet<Record> run( Record query ) {
			Log.log.debug("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
			ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
			int minCount = (int)Math.ceil(theta*query.getMinTransLength());
			Log.log.trace("minCount=%d", ()->minCount);
			Object2ObjectMap<Record, IntList> rec2idxListMap = getCommonTokenIdxLists(query);
			for ( Entry<Record, IntList> entry : rec2idxListMap.entrySet() ) {
				Record rec = entry.getKey();
				if ( entry.getValue().size() < minCount ) continue;
				IntList idxList = entry.getValue();
				idxList.sort(Integer::compare);
				Log.log.trace("idxList=%s", ()->idxList);
				Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
				ObjectList<Record> segmentList =  pruneSingleRecord(query, rec, idxList, minCount);
				candRecordSet.addAll(segmentList);
			}
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
		
		private ObjectList<Record> pruneSingleRecord( Record query, Record rec, IntList idxList, int minCount ) {
			ObjectList<IntRange> segmentRangeList = findSegments(query, rec, idxList, theta);
			Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
			ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList, idxList, minCount );
			return segmentList;
		}

		private ObjectList<IntRange> findSegments( Record query, Record rec, IntList idxList, double theta ) {
			int m = idxList.size();
			ObjectList<IntRange> rangeList = new ObjectArrayList<>();
			for ( int i=0; i<m; ++i ) {
				int sidx = idxList.get(i);
				int num = 0;
				for ( int j=i; j<m; ++j ) {
					int eidx1 = idxList.get(j);
					++num;
					double score = (double)num/Math.max(query.getMinTransLength(), eidx1-sidx+1);
//					Log.log.trace("sidx=%d, eidx1=%d, score=%.3f, theta=%.3f", ()->sidx, ()->eidx1, ()->score, ()->theta);
					if ( score >= theta ) {
						if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx1;
						else rangeList.add(new IntRange(sidx, eidx1));
						Log.log.trace("range=%s", ()->rangeList.get(rangeList.size()-1));
					}
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
		
		private ObjectList<Record> splitRecord( Record rec, ObjectList<IntRange> segmentRangeList, IntList idxList, int minCount ) {
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( IntRange range : segmentRangeList ) {
					int count = 0;
					for ( int idx : idxList ) {
						if ( range.min <= idx && idx <= range.max ) ++count;
					}
					if ( count >= minCount ) segmentList.add(rec.getSubrecord(range.min, range.max+1));
				}
			}
			return segmentList;
		}
	} // end class QuerySideFilter
	
	@Override
	public ObjectSet<Record> textSideFilter( Record query ) {
		TextSideFilter filter = new TextSideFilter();
		return filter.run(query);
	}
	
	private class TextSideFilter {
		
		public ObjectSet<Record> run( Record query ) {
			Log.log.debug("PositionalIndexBasedFilter.textSideFilter(%d)", ()->query.getID());
			ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
			int minCount = (int)Math.ceil(theta*query.size());
			Log.log.trace("minCount=%d", ()->minCount);
			Object2ObjectMap<Record, PosListPair> rec2idxListMap = getCommonTokenIdxLists(query);
			for ( Entry<Record, PosListPair> e : rec2idxListMap.entrySet() ) {
				Record rec = e.getKey();
				statContainer.startWatch("Time_TS_IndexFilter.preprocess");
				rec.preprocessApplicableRules();
				rec.preprocessSuffixApplicableRules();
				statContainer.stopWatch("Time_TS_IndexFilter.preprocess");
				if ( e.getValue().nToken < minCount ) continue;
				double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
				int modifiedMinCount = (int)Math.ceil(modifiedTheta*query.size());
				IntList prefixIdxList = IntArrayList.wrap(e.getValue().prefixList.toIntArray());
				IntList suffixIdxList = IntArrayList.wrap(e.getValue().suffixList.toIntArray());
				statContainer.startWatch("Time_TS_IndexFilter.sortIdxList");
				prefixIdxList.sort(Integer::compareTo);
				suffixIdxList.sort(Integer::compareTo);
				statContainer.stopWatch("Time_TS_IndexFilter.sortIdxList");
				statContainer.startWatch("Time_TS_IndexFilter.findSegmentRanges");
				ObjectList<IntRange> segmentRangeList = findSegmentRanges(query, rec, prefixIdxList, suffixIdxList, modifiedTheta);
				statContainer.stopWatch("Time_TS_IndexFilter.findSegmentRanges");
				Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
				statContainer.startWatch("Time_TS_IndexFilter.splitRecord");
				ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList, prefixIdxList, suffixIdxList, modifiedMinCount);
				statContainer.stopWatch("Time_TS_IndexFilter.splitRecord");
				candRecordSet.addAll(segmentList);
			}
			return candRecordSet;
		}
		
		private Object2ObjectMap<Record, PosListPair> getCommonTokenIdxLists( Record query ) {
			Object2ObjectMap<Record, PosListPair> rec2idxListMap = new Object2ObjectOpenHashMap<>();
			IntSet candTokenSet = new IntOpenHashSet(query.getTokens());
			for ( int token : candTokenSet ) {
				ObjectList<InvListEntry> invList = index.getInvList(token);
				Log.log.debug("getCommonTokenIdxLists\ttoken=%d, len(invList)=%d", token, invList==null?0:invList.size());
				if ( invList != null ) {
					for ( InvListEntry e : invList ) {
						if ( !rec2idxListMap.containsKey(e.rec) ) rec2idxListMap.put(e.rec, new PosListPair());
						rec2idxListMap.get(e.rec).nToken += 1;
						rec2idxListMap.get(e.rec).prefixList.add(e.pos);
						rec2idxListMap.get(e.rec).suffixList.add(e.pos);
					}
				}
				ObjectList<TransInvListEntry> transInvList = index.getTransInvList(token);
				Log.log.debug("getCommonTokenIdxLists\ttoken=%d, len(transInvList)=%d", token, transInvList==null?0:transInvList.size());
				if ( transInvList != null ) {
					for ( TransInvListEntry e : transInvList ) {
						if ( !rec2idxListMap.containsKey(e.rec) ) rec2idxListMap.put(e.rec, new PosListPair());
						rec2idxListMap.get(e.rec).nToken += 1;
						rec2idxListMap.get(e.rec).prefixList.add(e.left);
						rec2idxListMap.get(e.rec).suffixList.add(e.right);
					}
				}
			}
			
			return rec2idxListMap;
		}

		private ObjectList<IntRange> findSegmentRanges( Record query, Record rec, IntList prefixIdxList, IntList suffixIdxList, double theta ) {
			int minPrefixIdx = prefixIdxList.getInt(0);
			int maxSuffixIdx = suffixIdxList.getInt(suffixIdxList.size()-1);
//			System.out.println("minPrefixIdx: "+minPrefixIdx+", maxSuffixIdx: "+maxSuffixIdx);
			statContainer.startWatch("Time_TS_IndexFilter.boundCalculator");
			TransLenCalculator boundCalculator = new TransLenCalculator(null, rec, minPrefixIdx, maxSuffixIdx, theta);
			statContainer.stopWatch("Time_TS_IndexFilter.boundCalculator");
			ObjectList<IntRange> rangeList = new ObjectArrayList<>();
			for ( int i=0; i<prefixIdxList.size(); ++i ) {
				int sidx = prefixIdxList.get(i);
				int num = 0;
				for ( int j=0; j<suffixIdxList.size(); ++j ) {
					int eidx = suffixIdxList.get(j);
					if ( eidx < sidx ) {
						continue;
					}
					++num;
					double score = (double)num/Math.max(query.size(), boundCalculator.getLB(sidx, eidx));
					if ( score >= theta ) {
						if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx;
						else rangeList.add(new IntRange(sidx, eidx));
					}
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

		private ObjectList<Record> splitRecord( Record rec, ObjectList<IntRange> segmentRangeList, IntList prefixIdxList, IntList suffixIdxList, int minCount ) {
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( IntRange range : segmentRangeList ) {
					int count = 0;
					IntList prefixIdxSubList = new IntArrayList();
					for ( int pos : prefixIdxList ) {
						if ( range.min > pos ) continue;
						if ( pos > range.max ) break;
						prefixIdxSubList.add(pos-range.min);
						++count;
					}
					IntList suffixIdxSubList = new IntArrayList();
					for ( int pos : suffixIdxList ) {
						if ( range.min > pos ) continue;
						if ( pos > range.max ) break;
						suffixIdxSubList.add(pos-range.min);
					}
					if ( count >= minCount ) {
						RecordWithPos segment = rec.getSubrecordWithPos(range.min, range.max+1, prefixIdxSubList, suffixIdxSubList);
						segmentList.add(segment);
					}
				}
			}
			return segmentList;
		}

		private class PosListPair {
			int nToken = 0;
			IntSet prefixList = new IntOpenHashSet();
			IntSet suffixList = new IntOpenHashSet();
		}

		@SuppressWarnings("unused")
		private String visualizeCandRecord( Record rec, IntList idxList ) {
			StringBuilder strbld = new StringBuilder();
			for ( int i=0, j=0; i<rec.size(); ++i ) {
				int count = 0;
				while ( j < idxList.size() && i == idxList.get(j) ) {
					++j;
					++count;
				}
				if ( count == 0 ) strbld.append("-");
				else if ( count > 9 ) strbld.append("O");
				else strbld.append(count);
			}
			return strbld.toString();
		}
	} // end class TextSideFilter
}

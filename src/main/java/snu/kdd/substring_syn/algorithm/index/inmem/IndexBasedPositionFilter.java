package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalIndexInterface;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalInvertedIndex;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithPos;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class IndexBasedPositionFilter extends AbstractIndexBasedFilter implements DiskBasedPositionalIndexInterface {

	protected final DiskBasedPositionalInvertedIndex index;
	
	public IndexBasedPositionFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(dataset, theta, statContainer);
		index = new DiskBasedPositionalInvertedIndex(dataset.getIndexedList());
	}

	@Override
	public long invListSize() { return index.invListSize(); }
	
	@Override
	public long transInvListSize() { return index.transInvListSize(); }
	
	@Override
	public final int getNumInvFault() { return index.getNumInvFault(); }

	@Override
	public final int getNumTinvFault() { return index.getNumTinvFault(); }
	
	@Override
	public ObjectList<Record> querySideFilter( Record query ) {
		QuerySideFilter filter = new QuerySideFilter();
		return filter.run(query);
	}
	
	private class QuerySideFilter {
		
		public ObjectList<Record> run( Record query ) {
//			Log.log.trace("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
			ObjectList<Record> candRecordSet = new ObjectArrayList<>();
			int minCount = (int)Math.ceil(theta*query.getMinTransLength());
//			Log.log.trace("minCount=%d", ()->minCount);
			Int2ObjectMap<IntList> rec2idxListMap = getCommonTokenIdxLists(query);
			for ( Int2ObjectMap.Entry<IntList> entry : rec2idxListMap.int2ObjectEntrySet() ) {
				if ( entry.getValue().size() < minCount ) continue;
				int ridx = entry.getIntKey();
				Record rec = dataset.getRecord(ridx);
				IntList idxList = entry.getValue();
				idxList.sort(Integer::compare);
//				Log.log.trace("idxList=%s", ()->idxList);
//				Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
				ObjectList<Record> segmentList =  pruneSingleRecord(query, rec, idxList, minCount);
				candRecordSet.addAll(segmentList);
			}
			return candRecordSet;
		}
		
		private Int2ObjectMap<IntList> getCommonTokenIdxLists( Record query ) {
			Int2ObjectMap<IntList> rec2idxListMap = new Int2ObjectOpenHashMap<IntList>();
			IntSet candTokenSet = query.getCandTokenSet();
			for ( int token : candTokenSet ) {
				ObjectList<InvListEntry> invList = index.getInvList(token);
				if ( invList == null ) continue; 
				for ( InvListEntry e : invList ) {
					int ridx = e.ridx;
					int pos = e.pos;
					if ( !rec2idxListMap.containsKey(ridx) ) rec2idxListMap.put(ridx, new IntArrayList());
					rec2idxListMap.get(ridx).add(pos);
				}
			}
			return rec2idxListMap;
		}
		
		private ObjectList<Record> pruneSingleRecord( Record query, Record rec, IntList idxList, int minCount ) {
			ObjectList<IntRange> segmentRangeList = findSegments(query, rec, idxList, theta);
//			Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
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
//						Log.log.trace("range=%s", ()->rangeList.get(rangeList.size()-1));
					}
				}
			}
//			Log.log.trace("rangeList=%s", ()->rangeList);
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
	public ObjectList<Record> textSideFilter( Record query ) {
		TextSideFilter filter = new TextSideFilter();
		return filter.run(query);
	}
	
	private class TextSideFilter {
		
		public ObjectList<Record> run( Record query ) {
//			Log.log.trace("PositionalIndexBasedFilter.textSideFilter(%d)", ()->query.getID());
			ObjectList<Record> candRecordSet = new ObjectArrayList<>();
			int minCount = (int)Math.ceil(theta*query.size());
//			Log.log.trace("minCount=%d", ()->minCount);
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists(query);
			for ( Int2ObjectMap.Entry<PosListPair> e : rec2idxListMap.int2ObjectEntrySet() ) {
				if ( e.getValue().nToken < minCount ) continue;
				int ridx = e.getIntKey();
				statContainer.startWatch("Time_TS_IndexFilter.getIdxList");
				IntList prefixIdxList = IntArrayList.wrap(e.getValue().prefixList.toIntArray());
				IntList suffixIdxList = IntArrayList.wrap(e.getValue().suffixList.toIntArray());
				statContainer.stopWatch("Time_TS_IndexFilter.getIdxList");
				statContainer.startWatch("Time_TS_IndexFilter.sortIdxList");
				prefixIdxList.sort(Integer::compareTo);
				suffixIdxList.sort(Integer::compareTo);
				int minPrefixIdx = prefixIdxList.getInt(0);
				int maxSuffixIdx = suffixIdxList.getInt(suffixIdxList.size()-1);
				statContainer.stopWatch("Time_TS_IndexFilter.sortIdxList");
				statContainer.startWatch("Time_TS_IndexFilter.getRecord");
				Record rec = dataset.getRecord(ridx).getSubrecord(minPrefixIdx, maxSuffixIdx+1);
				statContainer.stopWatch("Time_TS_IndexFilter.getRecord");
				addToIntList(prefixIdxList, -minPrefixIdx);
				addToIntList(suffixIdxList, -minPrefixIdx);
				statContainer.startWatch("Time_TS_IndexFilter.preprocess");
				rec.preprocessApplicableRules();
				rec.preprocessSuffixApplicableRules();
				statContainer.stopWatch("Time_TS_IndexFilter.preprocess");
				double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
				statContainer.startWatch("Time_TS_IndexFilter.transLen");
				TransLenCalculator transLen = new TransLenCalculator(null, rec, modifiedTheta);
				statContainer.stopWatch("Time_TS_IndexFilter.transLen");
				statContainer.startWatch("Time_TS_IndexFilter.findSegmentRanges");
				ObjectList<IntRange> segmentRangeList = findSegmentRanges(query, rec, prefixIdxList, suffixIdxList, transLen, modifiedTheta);
				statContainer.stopWatch("Time_TS_IndexFilter.findSegmentRanges");
//				Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
				statContainer.startWatch("Time_TS_IndexFilter.splitRecord");
				ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList, prefixIdxList, suffixIdxList, transLen, minCount);
				statContainer.stopWatch("Time_TS_IndexFilter.splitRecord");
				statContainer.startWatch("Time_TS_IndexFilter.addAllCands");
				candRecordSet.addAll(segmentList);
				statContainer.stopWatch("Time_TS_IndexFilter.addAllCands");
			}
			return candRecordSet;
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists( Record query ) {
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<>();
			IntSet candTokenSet = new IntOpenHashSet(query.getTokens());
			for ( int token : candTokenSet ) {
				ObjectList<InvListEntry> invList = index.getInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%d, len(invList)=%d", ()->token, ()->invList==null?0:invList.size());
				if ( invList != null ) {
					for ( InvListEntry e : invList ) {
						if ( !rec2idxListMap.containsKey(e.ridx) ) rec2idxListMap.put(e.ridx, new PosListPair());
						rec2idxListMap.get(e.ridx).nToken += 1;
						rec2idxListMap.get(e.ridx).prefixList.add(e.pos);
						rec2idxListMap.get(e.ridx).suffixList.add(e.pos);
					}
				}
				ObjectList<TransInvListEntry> transInvList = index.getTransInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%d, len(transInvList)=%d", ()->token, ()->transInvList==null?0:transInvList.size());
				if ( transInvList != null ) {
					for ( TransInvListEntry e : transInvList ) {
						if ( !rec2idxListMap.containsKey(e.ridx) ) rec2idxListMap.put(e.ridx, new PosListPair());
						rec2idxListMap.get(e.ridx).nToken += 1;
						rec2idxListMap.get(e.ridx).prefixList.add(e.left);
						rec2idxListMap.get(e.ridx).suffixList.add(e.right);
					}
				}
			}
			
			return rec2idxListMap;
		}
		
		private void addToIntList( IntList list, int c ) {
			for ( int i=0; i<list.size(); ++i ) list.set(i, list.get(i)+c);
		}

		private ObjectList<IntRange> findSegmentRanges( Record query, Record rec, IntList prefixIdxList, IntList suffixIdxList, TransLenCalculator transLen, double theta ) {
//			System.out.println("minPrefixIdx: "+minPrefixIdx+", maxSuffixIdx: "+maxSuffixIdx);
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
					double score = (double)num/Math.max(query.size(), transLen.getLB(sidx, eidx));
					if ( score >= theta ) {
						if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx;
						else rangeList.add(new IntRange(sidx, eidx));
					}
				}
			}
//			Log.log.trace("rangeList=%s", ()->rangeList);
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

		private ObjectList<Record> splitRecord( Record rec, ObjectList<IntRange> segmentRangeList, IntList prefixIdxList, IntList suffixIdxList, TransLenCalculator transLen, int minCount ) {
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( IntRange range : segmentRangeList ) {
					int count = 0;
					IntList prefixIdxSubList = new IntArrayList();
					for ( int pos : prefixIdxList ) {
						if ( range.min > pos ) continue;
						if ( pos > range.max ) break;
						prefixIdxSubList.add(pos-range.min);
					}
					IntList suffixIdxSubList = new IntArrayList();
					for ( int pos : suffixIdxList ) {
						if ( range.min > pos ) continue;
						if ( pos > range.max ) break;
						if ( suffixIdxSubList.size() == 0 || pos-range.min != suffixIdxSubList.get(suffixIdxSubList.size()-1) ) suffixIdxSubList.add(pos-range.min);
						++count;
					}
					if ( count >= minCount ) {
						Subrecord subrec = new Subrecord(rec, range.min, range.max+1);
						RecordWithPos segment = new RecordWithPos(Subrecord.toRecord(subrec), prefixIdxSubList, suffixIdxSubList);
						segmentList.add(segment);
					}
				}
			}
			return segmentList;
		}

		private class PosListPair {
			int nToken = 0;
			IntSet prefixList = new IntOpenHashSet();
			IntList suffixList = new IntArrayList();
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

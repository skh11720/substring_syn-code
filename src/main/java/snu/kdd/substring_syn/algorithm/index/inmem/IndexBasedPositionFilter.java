package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithPos;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.MaxBoundTokenCounter;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class IndexBasedPositionFilter extends AbstractIndexBasedFilter implements DiskBasedPositionalIndexInterface {

	protected final DiskBasedPositionalInvertedIndex index;
	private static final double EPS = 1e-5;
	
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
		QuerySideFilter filter = new QuerySideFilter(query);
		return filter.run();
	}
	
	private class QuerySideFilter {

		final Record query;
		final IntSet candTokenSet;
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		
		public QuerySideFilter( Record query ) {
			this.query = query;
			candTokenSet = query.getCandTokenSet();
			tokenCounter = new MaxBoundTokenCounter(candTokenSet);
			minCount = (int)Math.ceil(theta*query.getMinTransLength());
		}
		
		public ObjectList<Record> run() {
//			Log.log.trace("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
			ObjectList<Record> candRecordSet = new ObjectArrayList<>();
//			Log.log.trace("minCount=%d", ()->minCount);
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			for ( Int2ObjectMap.Entry<PosListPair> entry : rec2idxListMap.int2ObjectEntrySet() ) {
				if ( entry.getValue().nToken < minCount ) continue;
				int ridx = entry.getIntKey();
				Record rec = dataset.getRecord(ridx);
				IntList idxList = entry.getValue().idxList;
				idxList.sort(Integer::compare);
//				Log.log.trace("idxList=%s", ()->idxList);
//				Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
				ObjectList<Record> segmentList =  pruneSingleRecord(rec, idxList, minCount);
				candRecordSet.addAll(segmentList);
			}
			return candRecordSet;
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists() {
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<PosListPair>();
			for ( int token : candTokenSet ) {
				int nMax = tokenCounter.getMax(token);
				Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
				tokenCounter.clear();
				ObjectList<InvListEntry> invList = index.getInvList(token);
				if ( invList == null ) continue; 
				for ( InvListEntry e : invList ) {
					int ridx = e.ridx;
					int pos = e.pos;
					if ( !rec2idxListMap.containsKey(ridx) ) rec2idxListMap.put(ridx, new PosListPair());
					PosListPair pair = rec2idxListMap.get(ridx);
					if ( counter.get(ridx) < nMax ) {
						counter.addTo(ridx, 1);
						pair.nToken += 1;
					}
					pair.idxList.add(pos);
				}
			}
			return rec2idxListMap;
		}
		
		private ObjectList<Record> pruneSingleRecord( Record rec, IntList idxList, int minCount ) {
			ObjectList<IntRange> segmentRangeList = findSegments(rec, idxList, theta);
//			Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
			ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList, idxList, minCount );
			return segmentList;
		}

		private ObjectList<IntRange> findSegments( Record rec, IntList idxList, double theta ) {
			int m = idxList.size();
			ObjectList<IntRange> rangeList = new ObjectArrayList<>();
			for ( int i=0; i<m; ++i ) {
				int sidx = idxList.get(i);
				tokenCounter.clear();
				for ( int j=i; j<m; ++j ) {
					int eidx = idxList.get(j);
					int token = rec.getToken(eidx);
					tokenCounter.tryIncrement(token);
					int num = tokenCounter.sum();
					final double score;
					if ( Math.min(query.getMinTransLength(), eidx-sidx+1) >= num )  score = (double)num/(query.getMinTransLength() + eidx-sidx+1 - num) + EPS;
					else score = (double)num/Math.max(query.getMinTransLength(), eidx-sidx+1) + EPS;
//					Log.log.trace("sidx=%d, eidx1=%d, score=%.3f, theta=%.3f", ()->sidx, ()->eidx1, ()->score, ()->theta);
					if ( score >= theta ) {
						if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx;
						else rangeList.add(new IntRange(sidx, eidx));
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
					tokenCounter.clear();
					for ( int idx : idxList ) {
						if ( range.min <= idx && idx <= range.max ) tokenCounter.tryIncrement(rec.getToken(idx));
					}
					if ( tokenCounter.sum() >= minCount ) segmentList.add(rec.getSubrecord(range.min, range.max+1));
				}
			}
			return segmentList;
		}

		private class PosListPair {
			int nToken = 0;
			IntList idxList = new IntArrayList();
		}
	} // end class QuerySideFilter
	
	@Override
	public ObjectList<Record> textSideFilter( Record query ) {
		TextSideFilter filter = new TextSideFilter(query);
		return filter.run();
	}
	
	private class TextSideFilter {

		final Record query;
		final IntSet candTokenSet;
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		
		public TextSideFilter( Record query ) {
			this.query = query;;
			candTokenSet = new IntOpenHashSet(query.getTokens());
			tokenCounter = new MaxBoundTokenCounter(candTokenSet);
			minCount = (int)Math.ceil(theta*query.size());
		}
		
		public ObjectList<Record> run() {
//			Log.log.trace("PositionalIndexBasedFilter.textSideFilter(%d)", ()->query.getID());
			ObjectList<Record> candRecordSet = new ObjectArrayList<>();
//			Log.log.trace("minCount=%d", ()->minCount);
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			for ( Int2ObjectMap.Entry<PosListPair> e : rec2idxListMap.int2ObjectEntrySet() ) {
				if ( e.getValue().nToken < minCount ) continue;
				int ridx = e.getIntKey();
				statContainer.startWatch("Time_TS_IndexFilter.getIdxList");
				IntList prefixIdxList = IntArrayList.wrap(e.getValue().prefixList.toIntArray());
				ObjectList<IntPair> suffixTokenList = e.getValue().suffixTokenList;
//				IntList suffixIdxList = IntArrayList.wrap(e.getValue().suffixList.toIntArray());
				statContainer.stopWatch("Time_TS_IndexFilter.getIdxList");
				statContainer.startWatch("Time_TS_IndexFilter.sortIdxList");
				prefixIdxList.sort(Integer::compareTo);
				suffixTokenList.sort((x,y)->Integer.compare(x.i1, y.i1));
				IntList suffixIdxList = new IntArrayList();
				IntList tokenList = new IntArrayList();
				Util.unzip(suffixTokenList, suffixIdxList, tokenList);
//				suffixIdxList.sort(Integer::compareTo);
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
				ObjectList<IntRange> segmentRangeList = findSegmentRanges(rec, prefixIdxList, suffixIdxList, tokenList, transLen, modifiedTheta);
				statContainer.stopWatch("Time_TS_IndexFilter.findSegmentRanges");
//				Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
				statContainer.startWatch("Time_TS_IndexFilter.splitRecord");
				ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList, prefixIdxList, suffixIdxList, tokenList, transLen, minCount);
				statContainer.stopWatch("Time_TS_IndexFilter.splitRecord");
				statContainer.startWatch("Time_TS_IndexFilter.addAllCands");
				candRecordSet.addAll(segmentList);
				statContainer.stopWatch("Time_TS_IndexFilter.addAllCands");
			}
			return candRecordSet;
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists() {
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<>();
			for ( int token : candTokenSet ) {
				int nMax = tokenCounter.getMax(token);
				Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
				ObjectList<InvListEntry> invList = index.getInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%d, len(invList)=%d", ()->token, ()->invList==null?0:invList.size());
				if ( invList != null ) {
					for ( InvListEntry e : invList ) {
						if ( !rec2idxListMap.containsKey(e.ridx) ) rec2idxListMap.put(e.ridx, new PosListPair());
						PosListPair pair = rec2idxListMap.get(e.ridx);
						if ( counter.get(e.ridx) < nMax ) {
							counter.addTo(e.ridx, 1);
							pair.nToken += 1;
						}
						pair.prefixList.add(e.pos);
						pair.suffixTokenList.add(new IntPair(e.pos, token));
					}
				}
				ObjectList<TransInvListEntry> transInvList = index.getTransInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%d, len(transInvList)=%d", ()->token, ()->transInvList==null?0:transInvList.size());
				if ( transInvList != null ) {
					for ( TransInvListEntry e : transInvList ) {
						if ( !rec2idxListMap.containsKey(e.ridx) ) rec2idxListMap.put(e.ridx, new PosListPair());
						PosListPair pair = rec2idxListMap.get(e.ridx);
						if ( counter.get(e.ridx) < nMax ) {
							counter.addTo(e.ridx, 1);
							pair.nToken += 1;
						}
						pair.prefixList.add(e.left);
						pair.suffixTokenList.add(new IntPair(e.right, token));
					}
				}
			}
			
			return rec2idxListMap;
		}
		
		private void addToIntList( IntList list, int c ) {
			for ( int i=0; i<list.size(); ++i ) list.set(i, list.get(i)+c);
		}

		private ObjectList<IntRange> findSegmentRanges( Record rec, IntList prefixIdxList, IntList suffixIdxList, IntList tokenList, TransLenCalculator transLen, double theta ) {
//			System.out.println("minPrefixIdx: "+minPrefixIdx+", maxSuffixIdx: "+maxSuffixIdx);
			ObjectList<IntRange> rangeList = new ObjectArrayList<>();
			for ( int i=0; i<prefixIdxList.size(); ++i ) {
				int sidx = prefixIdxList.get(i);
				tokenCounter.clear();
				for ( int j=0; j<suffixIdxList.size(); ++j ) {
					int eidx = suffixIdxList.get(j);
					if ( eidx < sidx ) continue;
					int token = tokenList.get(j);
					tokenCounter.tryIncrement(token);
					int num = tokenCounter.sum();
					final double score;
					if ( Math.min(query.size(), transLen.getLB(sidx, eidx)) >= num ) score = (double)num/(query.size() + transLen.getLB(sidx, eidx) - num) + EPS;
					else score = (double)num/Math.max(query.size(), transLen.getLB(sidx, eidx)) + EPS;
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

		private ObjectList<Record> splitRecord( Record rec, ObjectList<IntRange> segmentRangeList, IntList prefixIdxList, IntList suffixIdxList, IntList tokenList, TransLenCalculator transLen, int minCount ) {
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( IntRange range : segmentRangeList ) {
					tokenCounter.clear();
					IntList prefixIdxSubList = new IntArrayList();
					for ( int pos : prefixIdxList ) {
						if ( range.min > pos ) continue;
						if ( pos > range.max ) break;
						prefixIdxSubList.add(pos-range.min);
					}
					IntList suffixIdxSubList = new IntArrayList();
					for ( int j=0; j<suffixIdxList.size(); ++j ) {
						int pos = suffixIdxList.get(j);
						int token = tokenList.get(j);
						if ( range.min > pos ) continue;
						if ( pos > range.max ) break;
						if ( suffixIdxSubList.size() == 0 || pos-range.min != suffixIdxSubList.get(suffixIdxSubList.size()-1) ) suffixIdxSubList.add(pos-range.min);
						tokenCounter.tryIncrement(token);
					}
					if ( tokenCounter.sum() >= minCount ) {
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
			ObjectList<IntPair> suffixTokenList = new ObjectArrayList<IntPair>();
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

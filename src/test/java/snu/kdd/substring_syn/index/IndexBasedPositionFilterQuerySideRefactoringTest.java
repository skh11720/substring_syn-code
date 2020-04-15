package snu.kdd.substring_syn.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalInvertedIndex;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithEndpoints;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Int2IntBinaryHeap;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.MaxBoundTokenCounterDeprecated;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedPositionFilterQuerySideRefactoringTest {
	
	private static final double EPS = 1e-5;
	Dataset dataset = null;
	DiskBasedPositionalInvertedIndex index = null;
	StatContainer statContainer = new StatContainer();
	double theta = 0.6;
	boolean useCF = false;

	@Test
	public void test() throws IOException {
		DatasetParam param = new DatasetParam("AMAZON", "10000", "107836", "5", "1.0", "-1");
		theta = 0.6;
		dataset = DatasetFactory.createInstanceByName(param);
		index = new DiskBasedPositionalInvertedIndex(dataset.getIndexedList());
		StatContainer.global = new StatContainer();
		
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessApplicableRules();
			query.preprocessSuffixApplicableRules();
			Iterator<TransformableRecordInterface> iter0 = new QuerySideFilter0(query);
			Iterator<TransformableRecordInterface> iter1 = new QuerySideFilter(query);
			
			assert checkIteratorEquivalence(iter0, iter1);
		}
	}
	
	public static boolean checkMapEquivalence(Int2ObjectMap<QuerySideFilter.PosListPair> map0, Int2ObjectMap<QuerySideFilter.PosListPair> map1) {
		IntSet keySet0 = map0.keySet();
		IntSet keySet1 = map1.keySet();
		if ( !keySet0.equals(keySet1) ) {
			Log.log.error("keySet0 != keySet1");
			Log.log.error("keySet0="+keySet0);
			Log.log.error("keySet1="+keySet1);
			return false;
		}
		for ( int key : keySet0 ) {
			QuerySideFilter.PosListPair val0 = map0.get(key);
			QuerySideFilter.PosListPair val1 = map1.get(key);
			if ( val0 != val1 ) {
				Log.log.error("map0[%d] != map1[%d]", key);
				Log.log.error("map0[%d]=%d", val0);
				Log.log.error("map1[%d]=%d", val1);
				return false;
			}
		}
		return true;
	}
	
	public static boolean checkIteratorEquivalence(Iterator<TransformableRecordInterface> iter0, Iterator<TransformableRecordInterface> iter1) {
		while (iter0.hasNext()) {
			TransformableRecordInterface rec0 = iter0.next();
			if (!iter1.hasNext()) {
				Log.log.error("iter1 has less elements");
				return false;
			}
			TransformableRecordInterface rec1 = iter1.next();
//			Log.log.trace("check: rec0.idx=%d, rec1.idx=%d", rec0.getIdx(), rec1.getIdx());
			if ( rec0.getID() != rec1.getID() ) {
				Log.log.error("IDs are different");
				Log.log.error("rec0="+rec0);
				Log.log.error("rec1="+rec1);
				return false;
			}
			if ( rec0.getIdx() != rec1.getIdx() ) {
				Log.log.error("idxs are different");
				Log.log.error("rec0="+rec0);
				Log.log.error("rec1="+rec1);
				return false;
			}
			if ( rec0.size() != rec1.size() ) {
				Log.log.error("sizes are different");
				Log.log.error("rec0="+rec0);
				Log.log.error("rec1="+rec1);
				return false;
			}
			if ( !Arrays.equals(rec0.getTokenArray(), rec1.getTokenArray()) ) {
				Log.log.error("token arrays are different");
				Log.log.error("rec0="+rec0);
				Log.log.error("rec1="+rec1);
				return false;
			}
		}
		if (iter1.hasNext()) {
			Log.log.error("iter1 has more elements");
			return false;
		}
		return true;
	}

	class QuerySideFilter implements Iterator<TransformableRecordInterface> {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final Int2IntOpenHashMap candTokenCounter;
		final int minCount;
		final Iterator<PosListPair> iter;
		Iterator<Record> segmentIter = null;
		Record thisRec = null;
		Int2IntOpenHashMap tokenCounter;
		
		public QuerySideFilter( Record query ) {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			this.query = query;
			IntArrayList candTokenList = new IntArrayList();
			IntSet intSet = new IntOpenHashSet();
			for ( Rule r : query.getApplicableRuleIterable() ) {
				for ( int token : r.getRhs() ) {
					intSet.add(token);
					candTokenList.add(token);
				}
			}
			candTokenSet = new IntArrayList(intSet.stream().sorted().iterator());
			candTokenCounter = new Int2IntOpenHashMap();
			for ( int token : candTokenList ) candTokenCounter.addTo(token, 1);
			minCount = (int)Math.ceil(theta*query.getMinTransLength());

			statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");

			iter = new PosListPairIterator();
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
		}
		
		class PosListPairIterator implements Iterator<PosListPair> {
				
			Int2ObjectMap<PositionInvList> tok2listMap = new Int2ObjectOpenHashMap<PositionInvList>();
			Int2IntBinaryHeap heap = new Int2IntBinaryHeap();
			PosListPair e;
			int nEntries = 0;
			int nLists = 0;
			int sumListLen = 0;
			
			public PosListPairIterator() {
				for ( int token : candTokenSet ) {
					PositionInvList list = index.getInvList(token);
					if ( list != null ) {
						list.init();
						tok2listMap.put(token, list);
						heap.insert(list.getIdx(), token);
					}
				}
			}

			@Override
			public PosListPair next() {
				e = new PosListPair();
				tokenCounter = new Int2IntOpenHashMap();
				e.ridx = extractHead();
				while ( !heap.isEmpty() && heap.peekKey() == e.ridx ) extractHead();
				nLists += 1;
				sumListLen += e.idxList.size();
				return e;
			}
			
			private int extractHead() {
				IntPair head = heap.poll();
				nEntries += 1;
				int token = head.i2;
				int pos = tok2listMap.get(token).getPos();
				if ( tokenCounter.get(token) < candTokenCounter.get(token) ) {
					e.nToken += 1;
					tokenCounter.addTo(token, 1);
				}
				e.idxList.add(pos);
				getNextFromList(token);
				return head.i1;
			}
			
			private void getNextFromList(int token) {
				PositionInvList list = tok2listMap.get(token);
				list.next();
				if ( list.hasNext() ) heap.insert(list.getIdx(), token);
			}
			
			@Override
			public boolean hasNext() {
				return !heap.isEmpty();
			}
		}

		@Override
		public boolean hasNext() {
			return thisRec != null;
		}

		@Override
		public Record next() {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			Record rec = thisRec;
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
			return rec;
		}
		
		public Record findNext() {
			if ( segmentIter == null || !segmentIter.hasNext() ) {
				segmentIter = null;
				while ( iter.hasNext() ) {
					PosListPair entry = iter.next();
					if ( useCF && entry.nToken < minCount ) continue;
					int ridx = entry.ridx;
					statContainer.startWatch("Time_QS_IndexFilter.getRecord");
					Record rec = dataset.getRawRecord(ridx);
//					Log.log.trace("QuerySideFilter: rec.idx=%d, rec.id=%d, rec.size=%d, rec=%s", rec.getIdx(), rec.getID(), rec.size(), rec.toOriginalString());
					statContainer.stopWatch("Time_QS_IndexFilter.getRecord");
					IntList idxList = entry.idxList;
					statContainer.startWatch("Time_QS_IndexFilter.idxList.sort");
					idxList.sort(Integer::compare);
					statContainer.stopWatch("Time_QS_IndexFilter.idxList.sort");
	//				Log.log.trace("idxList=%s", ()->idxList);
	//				Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
					statContainer.startWatch("Time_QS_IndexFilter.pruneSingleRecord");
					segmentIter =  findSegments(rec, idxList, theta).iterator();
					statContainer.stopWatch("Time_QS_IndexFilter.pruneSingleRecord");
					if ( segmentIter.hasNext() ) break;
				}
				if ( segmentIter == null || !segmentIter.hasNext() ) return null;
			}
			return segmentIter.next();
		}
		
		private ObjectList<Record> findSegments( Record rec, IntList idxList, double theta ) {
//			Log.log.trace("QuerySideFilter: idxList=%s", idxList);
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			int m = idxList.size();
			for ( int i=0; i<m; ++i ) {
				int sidx = idxList.get(i);
				tokenCounter = new Int2IntOpenHashMap();
				int num = 0;
				MergedRange mrange = new MergedRange(sidx);
				for ( int j=i; j<m; ++j ) {
					int eidx = idxList.get(j);
					int token = rec.getToken(eidx);
					if ( tokenCounter.get(token) < candTokenCounter.get(token) ) {
						tokenCounter.addTo(token, 1);
						num += 1;
					}
					final double score;
					if ( query.getMinTransLength() < num ) score = (double)num/(eidx-sidx+1) + EPS;
					else score = (double)num/(query.getMinTransLength() + eidx-sidx+1 - num) + EPS;
//					Log.log.trace("sidx=%d, eidx1=%d, score=%.3f, theta=%.3f", ()->sidx, ()->eidx1, ()->score, ()->theta);
					if ( score >= theta ) {
						if ( !useCF || num >= minCount ) {
							mrange.eidxList.add(eidx+1);
						}
//						Log.log.trace("range=%s", ()->rangeList.get(rangeList.size()-1));
					}
				}
				if ( mrange.eidxList.size() > 0 ) {
					segmentList.add(new RecordWithEndpoints(rec, mrange.sidx, mrange.eidxList));
				}
			}
			return segmentList;
		}
		
		private class PosListPair {
			int ridx = 0;
			int nToken = 0;
			IntList idxList = new IntArrayList();
		}
	} // end class QuerySideFilter

	class QuerySideFilter0 implements Iterator<TransformableRecordInterface> {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final MaxBoundTokenCounterDeprecated tokenCounter;
		final int minCount;
		final Iterator<Entry<Integer, PosListPair>> iter;
		Iterator<Record> segmentIter = null;
		Record thisRec = null;
		
		public QuerySideFilter0( Record query ) {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			this.query = query;
			IntArrayList candTokenList = new IntArrayList();
			IntSet intSet = new IntOpenHashSet();
			for ( Rule r : query.getApplicableRuleIterable() ) {
				for ( int token : r.getRhs() ) {
					intSet.add(token);
					candTokenList.add(token);
				}
			}
			candTokenSet = new IntArrayList(intSet.stream().sorted().iterator());
			tokenCounter = new MaxBoundTokenCounterDeprecated(candTokenList);
			minCount = (int)Math.ceil(theta*query.getMinTransLength());

//			Log.log.trace("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
//			Log.log.trace("minCount=%d", ()->minCount);
			statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			iter = rec2idxListMap.entrySet().stream().sorted((e1,e2)->Integer.compare(e1.getKey(), e2.getKey())).iterator();
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
		}

		@Override
		public boolean hasNext() {
			return thisRec != null;
		}

		@Override
		public Record next() {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			Record rec = thisRec;
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
			return rec;
		}
		
		public Record findNext() {
			if ( segmentIter == null || !segmentIter.hasNext() ) {
				segmentIter = null;
				while ( iter.hasNext() ) {
					Entry<Integer, PosListPair> entry = iter.next();
					if ( useCF && entry.getValue().nToken < minCount ) continue;
					int ridx = entry.getKey();
					statContainer.startWatch("Time_QS_IndexFilter.getRecord");
					Record rec = dataset.getRawRecord(ridx);
//					Log.log.trace("QuerySideFilter: rec.idx=%d, rec.id=%d, rec.size=%d, rec=%s", rec.getIdx(), rec.getID(), rec.size(), rec.toOriginalString());
					statContainer.stopWatch("Time_QS_IndexFilter.getRecord");
					IntList idxList = entry.getValue().idxList;
					statContainer.startWatch("Time_QS_IndexFilter.idxList.sort");
					idxList.sort(Integer::compare);
					statContainer.stopWatch("Time_QS_IndexFilter.idxList.sort");
	//				Log.log.trace("idxList=%s", ()->idxList);
	//				Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
					statContainer.startWatch("Time_QS_IndexFilter.pruneSingleRecord");
					segmentIter =  findSegments(rec, idxList, theta).iterator();
					statContainer.stopWatch("Time_QS_IndexFilter.pruneSingleRecord");
					if ( segmentIter.hasNext() ) break;
				}
				if ( segmentIter == null || !segmentIter.hasNext() ) return null;
			}
			return segmentIter.next();
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists() {
			Log.log.trace("QuerySideFilter.getCommonTokenIdxLists()");
			long nEntries = 0;
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<PosListPair>();
//			int countUpperBound = tokenCounter.sumBounds();
			for ( int token : candTokenSet ) {
				tokenCounter.clear();
				PositionInvList invList = index.getInvList(token);
				if ( invList != null ) {
//					Log.log.trace("QuerySideFilter.getCommonTokenIdxLists: token=%s, invList.size=%d", ()->Record.tokenIndex.getToken(token), ()->invList.size());
//					if ( !useCF || countUpperBound >= minCount ) {
					statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.scan");
					// there is a chance that a record not seen until now can have at least minCount common tokens.
					for ( invList.init(); invList.hasNext(); invList.next() ) {
						int ridx = invList.getIdx();
						int pos = invList.getPos();
						if ( !rec2idxListMap.containsKey(ridx) ) rec2idxListMap.put(ridx, new PosListPair());
						PosListPair pair = rec2idxListMap.get(ridx);
						if ( tokenCounter.tryIncrement(ridx, token) ) {
							pair.nToken += 1;
						}
						pair.idxList.add(pos);
					}
					statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.scan");
//					}
//					else {
//						// all unseen records cannot be the answer by the count filtering so we ignore them.
//						// we use the binary search to update the count of only the records in rec2idxListMap.
//						statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.binarySearch");
//						for ( int ridx : rec2idxListMap.keySet() ) {
//							PosListPair pair = rec2idxListMap.get(ridx);
//							if ( pair.nToken + countUpperBound >= minCount ) {
//								int idx = Util.binarySearch(invList, ridx);
//								if ( idx >= 0 ) {
//									while ( idx < invList.size() && invList.getIdx(idx) == ridx ) {
//										if ( tokenCounter.tryIncrement(ridx, token) ) {
//											pair.nToken += 1;
//										}
//										pair.idxList.add(invList.getPos(idx));
//										idx += 1;
//									}
//								}
//							}
//						}
//						statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.binarySearch");
//					}
					nEntries += invList.size();
				}
//				countUpperBound -= tokenCounter.getMax(token);
			}
			Log.log.trace("QuerySideFilter: nEntries=%d", nEntries);
			Log.log.trace("QuerySideFilter: rec2idxListMap.size=%d", ()->rec2idxListMap.size());
			Log.log.trace("QuerySideFilter: rec2idxListMap.length=%d", ()->rec2idxListMap.values().stream().mapToInt(x->x.idxList.size()).sum());
			return rec2idxListMap;
		}
		
		private ObjectList<Record> findSegments( Record rec, IntList idxList, double theta ) {
//			Log.log.trace("QuerySideFilter: idxList=%s", idxList);
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			int m = idxList.size();
			for ( int i=0; i<m; ++i ) {
				int sidx = idxList.get(i);
				tokenCounter.clear();
				MergedRange mrange = new MergedRange(sidx);
				for ( int j=i; j<m; ++j ) {
					int eidx = idxList.get(j);
					int token = rec.getToken(eidx);
					tokenCounter.tryIncrement(token, token);
					int num = tokenCounter.sum();
					final double score;
					if ( query.getMinTransLength() < num ) score = (double)num/(eidx-sidx+1) + EPS;
					else score = (double)num/(query.getMinTransLength() + eidx-sidx+1 - num) + EPS;
//					Log.log.trace("sidx=%d, eidx1=%d, score=%.3f, theta=%.3f", ()->sidx, ()->eidx1, ()->score, ()->theta);
					if ( score >= theta ) {
						if ( !useCF || num >= minCount ) {
							mrange.eidxList.add(eidx+1);
						}
//						Log.log.trace("range=%s", ()->rangeList.get(rangeList.size()-1));
					}
				}
				if ( mrange.eidxList.size() > 0 ) {
					segmentList.add(new RecordWithEndpoints(rec, mrange.sidx, mrange.eidxList));
				}
			}
			return segmentList;
		}
		
		private class PosListPair {
			int nToken = 0;
			IntList idxList = new IntArrayList();
		}
	} // end class QuerySideFilter

	class MergedRange {
		final int sidx;
		final IntList eidxList = new IntArrayList();
		
		public MergedRange( int sidx ) {
			this.sidx = sidx;
		}
		
		@Override
		public String toString() {
			return "("+sidx+"\t"+eidxList+")";
		}
	}
}

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
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalInvertedIndex;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithEndpoints;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Int2IntBinaryHeap;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.MaxBoundTokenCounter;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class IndexBasedPositionFilterTextSideRefactoringTest {
	
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
			Iterator<TransformableRecordInterface> iter0 = new TextSideFilter0(query);
			Iterator<TransformableRecordInterface> iter1 = new TextSideFilter(query);
			
			assert checkIteratorEquivalence(iter0, iter1);
		}
	}
	
	public static boolean checkMapEquivalence(Int2ObjectMap<TextSideFilter.PosListPair> map0, Int2ObjectMap<TextSideFilter.PosListPair> map1) {
		IntSet keySet0 = map0.keySet();
		IntSet keySet1 = map1.keySet();
		if ( !keySet0.equals(keySet1) ) {
			Log.log.error("keySet0 != keySet1");
			Log.log.error("keySet0="+keySet0);
			Log.log.error("keySet1="+keySet1);
			return false;
		}
		for ( int key : keySet0 ) {
			TextSideFilter.PosListPair val0 = map0.get(key);
			TextSideFilter.PosListPair val1 = map1.get(key);
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

	class TextSideFilter implements Iterator<TransformableRecordInterface> {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final Int2IntOpenHashMap candTokenCounter;
		final int minCount;
		final Iterator<PosListPair> iter;
		Iterator<Record> segmentIter = null;
		Record thisRec = null;
		Int2IntOpenHashMap tokenCounter;
		
		public TextSideFilter( Record query ) {
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			this.query = query;;
			IntArrayList candTokenList = new IntArrayList();
			IntSet intSet = new IntOpenHashSet();
			for ( int token : query.getTokens() ) {
				intSet.add(token);
				candTokenList.add(token);
			}
			candTokenSet = new IntArrayList(intSet.stream().sorted().iterator());
			candTokenCounter = new Int2IntOpenHashMap();
			for ( int token : candTokenList ) candTokenCounter.addTo(token, 1);
			minCount = (int)Math.ceil(theta*query.size());

			statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
			statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");

			iter = new PosListPairIterator();
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
		}
		
		class PosListPairIterator implements Iterator<PosListPair> {
				
			Int2ObjectMap<PositionInvList> tok2listMap = new Int2ObjectOpenHashMap<PositionInvList>();
			Int2ObjectMap<PositionTrInvList> tok2trlistMap = new Int2ObjectOpenHashMap<PositionTrInvList>();
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
					PositionTrInvList trlist = index.getTransInvList(token);
					if ( trlist != null ) {
						trlist.init();
						tok2trlistMap.put(token, trlist);
						heap.insert(trlist.getIdx(), -token-1); // assume token >= 0
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
				sumListLen += e.prefixList.size();
				return e;
			}
			
			private int extractHead() {
				IntPair head = heap.poll();
				nEntries += 1;
				int token = head.i2;

				if ( token >= 0 ) {
					int pos = tok2listMap.get(token).getPos();
					e.prefixList.add(pos);
					e.suffixTokenList.add(new IntPair(pos, token));
					getNextFromList(token);
				}
				else {
					token = -token-1;
					PositionTrInvList list = tok2trlistMap.get(token);
					int left = list.getLeft();
					int right = list.getRight();
					e.prefixList.add(left);
					e.suffixTokenList.add(new IntPair(right, token));
					getNextFromTrList(token);
				}

				if ( tokenCounter.get(token) < candTokenCounter.get(token) ) {
					e.nToken += 1;
					tokenCounter.addTo(token, 1);
				}
				return head.i1;
			}
			
			private void getNextFromList(int token) {
				PositionInvList list = tok2listMap.get(token);
				list.next();
				if ( list.hasNext() ) heap.insert(list.getIdx(), token);
			}

			private void getNextFromTrList(int token) {
				PositionTrInvList list = tok2trlistMap.get(token);
				list.next();
				if ( list.hasNext() ) heap.insert(list.getIdx(), -token-1);
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
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			Record rec = thisRec;
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
			return rec;
		}
		
		public Record findNext() {
			if ( segmentIter == null || !segmentIter.hasNext() ) {
				segmentIter = null;
				while ( iter.hasNext() ) {
					PosListPair e = iter.next();
					if ( useCF && e.nToken < minCount ) continue;
					int ridx = e.ridx;
					statContainer.startWatch("Time_TS_IndexFilter.getIdxList");
					ObjectList<IntPair> suffixTokenList = e.suffixTokenList;
					IntList prefixIdxList = IntArrayList.wrap(e.prefixList.toIntArray());
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
					TransformableRecordInterface fullRec = dataset.getRecord(ridx);
//					Log.log.trace("TextSideFilter: fullRec.idx=%d, fullRec.id=%d, fullRec.size=%d, fullRec=%s", fullRec.getIdx(), fullRec.getID(), fullRec.size(), fullRec.toOriginalString());
//					Log.log.trace("prefixIdxList=%s", prefixIdxList);
//					Log.log.trace("suffixIdxList=%s", suffixIdxList);
					Subrecord rec = new Subrecord(fullRec, minPrefixIdx, maxSuffixIdx+1);
					statContainer.stopWatch("Time_TS_IndexFilter.getRecord");
					addToIntList(prefixIdxList, -minPrefixIdx);
					addToIntList(suffixIdxList, -minPrefixIdx);
	//				statContainer.startWatch("Time_TS_IndexFilter.preprocess");
	//				statContainer.stopWatch("Time_TS_IndexFilter.preprocess");
					double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
	//				statContainer.startWatch("Time_TS_IndexFilter.transLen");
	//				TransLenCalculator transLen = new TransLenCalculator(null, rec, modifiedTheta);
	//				statContainer.stopWatch("Time_TS_IndexFilter.transLen");
					statContainer.startWatch("Time_TS_IndexFilter.findSegmentRanges");
					segmentIter = findSegments(rec, prefixIdxList, suffixIdxList, tokenList, modifiedTheta).iterator();
					statContainer.stopWatch("Time_TS_IndexFilter.findSegmentRanges");
	//				Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
					if ( segmentIter.hasNext() ) break;
				}
				if ( segmentIter == null || !segmentIter.hasNext() ) return null;
			}
			return segmentIter.next();
		}
	
		private void addToIntList( IntList list, int c ) {
			for ( int i=0; i<list.size(); ++i ) list.set(i, list.get(i)+c);
		}

		private ObjectList<Record> findSegments( TransformableRecordInterface rec, IntList prefixIdxList, IntList suffixIdxList, IntList tokenList, double theta ) {
//			System.out.println("minPrefixIdx: "+minPrefixIdx+", maxSuffixIdx: "+maxSuffixIdx);
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			for ( int i=0; i<prefixIdxList.size(); ++i ) {
				int sidx = prefixIdxList.get(i);
				MergedRange mrange = new MergedRange(sidx);
				TransLenLazyCalculator transLen = new TransLenLazyCalculator(statContainer, rec, sidx, rec.size()-sidx, theta);
				tokenCounter.clear();
				int num = 0;
				for ( int j=0; j<suffixIdxList.size(); ++j ) {
					int eidx = suffixIdxList.get(j);
					if ( eidx < sidx ) continue;
					int token = tokenList.get(j);
					if ( tokenCounter.get(token) < candTokenCounter.get(token) ) {
						tokenCounter.addTo(token, 1);
						num += 1;
					}
					final double score;
					if ( transLen.getLB(eidx) < num ) score = (double)num/query.size() + EPS;
					else score = (double)num/(query.size() + transLen.getLB(eidx) - num) + EPS;
					if ( score >= theta ) {
						if ( !useCF || num >= minCount ) {
							if ( mrange.eidxList.size() == 0 || mrange.eidxList.getInt(mrange.eidxList.size()-1) < eidx+1 ) mrange.eidxList.add(eidx+1);
						}
					}
				}
				if ( mrange.eidxList.size() > 0 ) {
					Subrecord subrec = new Subrecord(rec, mrange.sidx, mrange.eidxList.getInt(mrange.eidxList.size()-1));
					addToIntList(mrange.eidxList, -mrange.sidx);
					segmentList.add(new RecordWithEndpoints(subrec.toRecord(), 0, mrange.eidxList));
				}
			}
			return segmentList;
		}

		private class PosListPair {
			int ridx = 0;
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

	class TextSideFilter0 implements Iterator<TransformableRecordInterface> {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		final Iterator<Entry<Integer, PosListPair>> iter;
		Iterator<Record> segmentIter = null;
		Record thisRec = null;
		
		public TextSideFilter0( Record query ) {
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			this.query = query;;
			IntArrayList candTokenList = new IntArrayList();
			IntSet intSet = new IntOpenHashSet();
			for ( int token : query.getTokens() ) {
				intSet.add(token);
				candTokenList.add(token);
			}
			candTokenSet = new IntArrayList(intSet.stream().sorted().iterator());
			tokenCounter = new MaxBoundTokenCounter(candTokenList);
			minCount = (int)Math.ceil(theta*query.size());

//			Log.log.trace("PositionalIndexBasedFilter.textSideFilter(%d)", ()->query.getID());
//			Log.log.trace("minCount=%d", ()->minCount);
			statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
//			Log.log.trace("rec2idxListMap.size=%d", ()->rec2idxListMap.size());
			iter = rec2idxListMap.entrySet().stream().sorted((e1,e2)->Integer.compare(e1.getKey(), e2.getKey())).iterator();
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
		}

		@Override
		public boolean hasNext() {
			return thisRec != null;
		}

		@Override
		public Record next() {
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			Record rec = thisRec;
			thisRec = findNext();
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
			return rec;
		}
		
		public Record findNext() {
			if ( segmentIter == null || !segmentIter.hasNext() ) {
				segmentIter = null;
				while ( iter.hasNext() ) {
					Entry<Integer, PosListPair> e = iter.next();
					if ( useCF && e.getValue().nToken < minCount ) continue;
					int ridx = e.getKey();
//					Log.log.trace("ridx=%d", ridx);
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
					TransformableRecordInterface fullRec = dataset.getRecord(ridx);
//					Log.log.trace("TextSideFilter: fullRec.idx=%d, fullRec.id=%d, fullRec.size=%d, fullRec=%s", fullRec.getIdx(), fullRec.getID(), fullRec.size(), fullRec.toOriginalString());
//					Log.log.trace("prefixIdxList=%s", prefixIdxList);
//					Log.log.trace("suffixIdxList=%s", suffixIdxList);
					Subrecord rec = new Subrecord(fullRec, minPrefixIdx, maxSuffixIdx+1);
					statContainer.stopWatch("Time_TS_IndexFilter.getRecord");
					addToIntList(prefixIdxList, -minPrefixIdx);
					addToIntList(suffixIdxList, -minPrefixIdx);
	//				statContainer.startWatch("Time_TS_IndexFilter.preprocess");
	//				statContainer.stopWatch("Time_TS_IndexFilter.preprocess");
					double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
	//				statContainer.startWatch("Time_TS_IndexFilter.transLen");
	//				TransLenCalculator transLen = new TransLenCalculator(null, rec, modifiedTheta);
	//				statContainer.stopWatch("Time_TS_IndexFilter.transLen");
					statContainer.startWatch("Time_TS_IndexFilter.findSegmentRanges");
					segmentIter = findSegments(rec, prefixIdxList, suffixIdxList, tokenList, modifiedTheta).iterator();
					statContainer.stopWatch("Time_TS_IndexFilter.findSegmentRanges");
	//				Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
					if ( segmentIter.hasNext() ) break;
				}
				if ( segmentIter == null || !segmentIter.hasNext() ) return null;
			}
			return segmentIter.next();
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists() {
			Log.log.trace("TextSideFilter.getCommonTokenIdxLists()");
			long nEntries = 0;
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<>();
//			int countUpperBound = tokenCounter.sumBounds();
			for ( int token : candTokenSet ) {
				tokenCounter.clear();
				PositionInvList invList = index.getInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%s (%d), len(invList)=%d", ()->Record.tokenIndex.getToken(token), ()->token, ()->invList==null?0:invList.size());
//				Log.log.trace("invList=%s", ()->invList);
				if ( invList != null ) {
//					Log.log.trace("TextSideFilter.getCommonTokenIdxLists: token=%s, invList.size=%d", ()->Record.tokenIndex.getToken(token), ()->invList.size());

//					if ( !useCF || countUpperBound >= minCount ) {
					statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.scan");
					for ( invList.init(); invList.hasNext(); invList.next() ) {
						int ridx = invList.getIdx();
						int pos = invList.getPos();
						//statContainer.startWatch("Time_TS_getCommon.rec2idxListMap1");
						if ( !rec2idxListMap.containsKey(ridx) ) rec2idxListMap.put(ridx, new PosListPair());
						//statContainer.stopWatch("Time_TS_getCommon.rec2idxListMap1");
						PosListPair pair = rec2idxListMap.get(ridx);
						//statContainer.startWatch("Time_TS_getCommon.counter_getAndAdd1");
						if ( tokenCounter.tryIncrement(ridx, token) ) {
							//statContainer.startWatch("Time_TS_getCommon.counter_addTo1");
							//statContainer.stopWatch("Time_TS_getCommon.counter_addTo1");
							pair.nToken += 1;
						}
						//statContainer.stopWatch("Time_TS_getCommon.counter_getAndAdd1");
						//statContainer.startWatch("Time_TS_getCommon.prefixListAdd1");
						pair.prefixList.add(pos);
						//statContainer.stopWatch("Time_TS_getCommon.prefixListAdd1");
						//statContainer.startWatch("Time_TS_getCommon.suffixTokenListAdd1");
						pair.suffixTokenList.add(new IntPair(pos, token));
						//statContainer.stopWatch("Time_TS_getCommon.suffixTokenListAdd1");
					}
					statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.scan");
//					}
//					else {
//						statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
//						for ( int ridx : rec2idxListMap.keySet() ) {
//							PosListPair pair = rec2idxListMap.get(ridx);
//							if ( pair.nToken + countUpperBound >= minCount ) {
//								int idx = Util.binarySearch(invList, ridx);
//								if ( idx >= 0 ) {
//									while ( idx < invList.size() && invList.getIdx(idx) == ridx ) {
//										if ( tokenCounter.tryIncrement(ridx, token) ) {
//											pair.nToken += 1;
//										}
//										pair.prefixList.add(invList.getPos(idx));
//										pair.suffixTokenList.add(new IntPair(invList.getPos(idx), token));
//										idx += 1;
//									}
//								}
//							}
//						}
//						statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
//					}
					nEntries += invList.size();
				} // end if invList

				PositionTrInvList transInvList = index.getTransInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%s (%d), len(transInvList)=%d", ()->Record.tokenIndex.getToken(token), ()->token, ()->transInvList==null?0:transInvList.size());
//				Log.log.trace("transInvList=%s", transInvList);
				if ( transInvList != null ) {
//					Log.log.trace("TextSideFilter.getCommonTokenIdxLists: token=%s, transInvList.size=%d", ()->Record.tokenIndex.getToken(token), ()->transInvList.size());

//					if ( !useCF || countUpperBound >= minCount ) {
					statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.scan");
					for ( transInvList.init(); transInvList.hasNext(); transInvList.next() ) {
						int ridx = transInvList.getIdx();
						int left = transInvList.getLeft();
						int right = transInvList.getRight();
						//statContainer.startWatch("Time_TS_getCommon.rec2idxListMap2");
						if ( !rec2idxListMap.containsKey(ridx) ) rec2idxListMap.put(ridx, new PosListPair());
						//statContainer.stopWatch("Time_TS_getCommon.rec2idxListMap2");
						PosListPair pair = rec2idxListMap.get(ridx);
						//statContainer.startWatch("Time_TS_getCommon.counter_getAndAdd2");
						if ( tokenCounter.tryIncrement(ridx, token) ) {
							//statContainer.startWatch("Time_TS_getCommon.counter_addTo2");
							//statContainer.stopWatch("Time_TS_getCommon.counter_addTo2");
							pair.nToken += 1;
						}
						//statContainer.stopWatch("Time_TS_getCommon.counter_getAndAdd2");
						//statContainer.startWatch("Time_TS_getCommon.prefixListAdd2");
						pair.prefixList.add(left);
						//statContainer.stopWatch("Time_TS_getCommon.prefixListAdd2");
						//statContainer.startWatch("Time_TS_getCommon.suffixTokenListAdd2");
						pair.suffixTokenList.add(new IntPair(right, token));
						//statContainer.stopWatch("Time_TS_getCommon.suffixTokenListAdd2");
					}
					statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.scan");
//					}
//					else {
//						statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
//						for ( int ridx : rec2idxListMap.keySet() ) {
//							PosListPair pair = rec2idxListMap.get(ridx);
//							if ( pair.nToken + countUpperBound >= minCount ) {
//								int idx = Util.binarySearch(transInvList, ridx);
//								if ( idx >= 0 ) {
//									while ( idx < transInvList.size() && transInvList.getIdx(idx) == ridx ) {
//										if ( tokenCounter.tryIncrement(ridx, token) ) {
//											pair.nToken += 1;
//										}
//										pair.prefixList.add(transInvList.getLeft(idx));
//										pair.suffixTokenList.add(new IntPair(transInvList.getRight(idx), token));
//										idx += 1;
//									}
//								}
//							}
//						}
//						statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
//					}
					nEntries += transInvList.size();
				} // end if transInvList
//				countUpperBound -= tokenCounter.getMax(token);
			} // end for token

			Log.log.trace("TextSideFilter: nEntries=%d", nEntries);
			Log.log.trace("TextSideFilter: rec2idxListMap.size=%d", ()->rec2idxListMap.size());
			Log.log.trace("TextSideFilter: rec2idxListMap.length=%d", ()->rec2idxListMap.values().stream().mapToInt(x->x.prefixList.size()).sum());
			return rec2idxListMap;
		}
		
		private void addToIntList( IntList list, int c ) {
			for ( int i=0; i<list.size(); ++i ) list.set(i, list.get(i)+c);
		}

		private ObjectList<Record> findSegments( TransformableRecordInterface rec, IntList prefixIdxList, IntList suffixIdxList, IntList tokenList, double theta ) {
//			System.out.println("minPrefixIdx: "+minPrefixIdx+", maxSuffixIdx: "+maxSuffixIdx);
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			for ( int i=0; i<prefixIdxList.size(); ++i ) {
				int sidx = prefixIdxList.get(i);
				MergedRange mrange = new MergedRange(sidx);
				TransLenLazyCalculator transLen = new TransLenLazyCalculator(statContainer, rec, sidx, rec.size()-sidx, theta);
				tokenCounter.clear();
				for ( int j=0; j<suffixIdxList.size(); ++j ) {
					int eidx = suffixIdxList.get(j);
					if ( eidx < sidx ) continue;
					int token = tokenList.get(j);
					tokenCounter.tryIncrement(token, token);
					int num = tokenCounter.sum();
					final double score;
					if ( transLen.getLB(eidx) < num ) score = (double)num/query.size() + EPS;
					else score = (double)num/(query.size() + transLen.getLB(eidx) - num) + EPS;
					if ( score >= theta ) {
						if ( !useCF || num >= minCount ) {
							if ( mrange.eidxList.size() == 0 || mrange.eidxList.getInt(mrange.eidxList.size()-1) < eidx+1 ) mrange.eidxList.add(eidx+1);
						}
					}
				}
				if ( mrange.eidxList.size() > 0 ) {
					Subrecord subrec = new Subrecord(rec, mrange.sidx, mrange.eidxList.getInt(mrange.eidxList.size()-1));
					addToIntList(mrange.eidxList, -mrange.sidx);
					segmentList.add(new RecordWithEndpoints(subrec.toRecord(), 0, mrange.eidxList));
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

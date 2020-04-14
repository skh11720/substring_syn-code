package snu.kdd.substring_syn.algorithm.index.inmem;

import java.math.BigInteger;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalIndexInterface;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalInvertedIndex;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithEndpoints;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Int2IntBinaryHeap;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class IndexBasedPositionFilter extends AbstractIndexBasedFilter implements DiskBasedPositionalIndexInterface {

	protected final DiskBasedPositionalInvertedIndex index;
	private static final double EPS = 1e-5;
	private final boolean useCF;
	
	public IndexBasedPositionFilter( Dataset dataset, double theta, boolean useCF, StatContainer statContainer ) {
		super(dataset, theta, statContainer);
		index = new DiskBasedPositionalInvertedIndex(dataset.getIndexedList());
		this.useCF = useCF;
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
	protected IntIterable querySideFilter(Record query) { return null; }
	
	@Override
	protected IntIterable textSideFilter(Record query) { return null; }
	
	@Override
	public Iterable<TransformableRecordInterface> getCandRecordsQuerySide( Record query ) {
		return new Iterable<TransformableRecordInterface>() {
			
			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				return new QuerySideFilter(query);
			}
		};
	}
	
	@SuppressWarnings("unused")
	private class QuerySideFilter implements Iterator<TransformableRecordInterface> {

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
	
	@Override
	public Iterable<TransformableRecordInterface> getCandRecordsTextSide( Record query ) {
		return new Iterable<TransformableRecordInterface>() {
			
			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				return new TextSideFilter(query);
			}
		};
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

	private class MergedRange {
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

	@Override
	public BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage();
	}
}

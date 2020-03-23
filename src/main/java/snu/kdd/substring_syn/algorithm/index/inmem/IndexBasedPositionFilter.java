package snu.kdd.substring_syn.algorithm.index.inmem;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map.Entry;

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
import snu.kdd.substring_syn.utils.MaxBoundTokenCounter;
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
	
	private class QuerySideFilter implements Iterator<TransformableRecordInterface> {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		final Iterator<Entry<Integer, PosListPair>> iter;
		Iterator<Record> segmentIter = null;
		Record thisRec = null;
		
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
			tokenCounter = new MaxBoundTokenCounter(candTokenList);
			minCount = (int)Math.ceil(theta*query.getMinTransLength());

//			Log.log.trace("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
//			Log.log.trace("minCount=%d", ()->minCount);
			statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			iter = rec2idxListMap.entrySet().iterator();
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
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<PosListPair>();
			int countUpperBound = tokenCounter.sumBounds();
			for ( int token : candTokenSet ) {
				tokenCounter.clear();
				PositionInvList invList = index.getInvList(token);
				if ( invList != null ) {
//					Log.log.trace("QuerySideFilter.getCommonTokenIdxLists: token=%s, invList.size=%d", ()->Record.tokenIndex.getToken(token), ()->invList.size());
					if ( !useCF || countUpperBound >= minCount ) {
						statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.scan");
						// there is a chance that a record not seen until now can have at least minCount common tokens.
						for ( int i=0; i<invList.size(); ++i ) {
							int ridx = invList.getIdx(i);
							int pos = invList.getPos(i);
							if ( !rec2idxListMap.containsKey(ridx) ) rec2idxListMap.put(ridx, new PosListPair());
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( tokenCounter.tryIncrement(ridx, token) ) {
								pair.nToken += 1;
							}
							pair.idxList.add(pos);
						}
						statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.scan");
					}
					else {
						// all unseen records cannot be the answer by the count filtering so we ignore them.
						// we use the binary search to update the count of only the records in rec2idxListMap.
						statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.binarySearch");
						for ( int ridx : rec2idxListMap.keySet() ) {
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( pair.nToken + countUpperBound >= minCount ) {
								int idx = Util.binarySearch(invList, ridx);
								if ( idx >= 0 ) {
									while ( idx < invList.size() && invList.getIdx(idx) == ridx ) {
										if ( tokenCounter.tryIncrement(ridx, token) ) {
											pair.nToken += 1;
										}
										pair.idxList.add(invList.getPos(idx));
										idx += 1;
									}
								}
							}
						}
						statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists.binarySearch");
					}
				}
				countUpperBound -= tokenCounter.getMax(token);
			}
//			Log.log.trace("QuerySideFilter: rec2idxListMap.size=%d", ()->rec2idxListMap.size());
//			Log.log.trace("QuerySideFilter: rec2idxListMap.length=%d", ()->rec2idxListMap.values().stream().mapToInt(x->x.idxList.size()).sum());
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
	
	@Override
	public Iterable<TransformableRecordInterface> getCandRecordsTextSide( Record query ) {
		return new Iterable<TransformableRecordInterface>() {
			
			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				return new TextSideFilter(query);
			}
		};
	}
	
	private class TextSideFilter implements Iterator<TransformableRecordInterface> {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		final Iterator<Entry<Integer, PosListPair>> iter;
		Iterator<Record> segmentIter = null;
		Record thisRec = null;
		
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
			tokenCounter = new MaxBoundTokenCounter(candTokenList);
			minCount = (int)Math.ceil(theta*query.size());

//			Log.log.trace("PositionalIndexBasedFilter.textSideFilter(%d)", ()->query.getID());
//			Log.log.trace("minCount=%d", ()->minCount);
			statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
//			Log.log.trace("rec2idxListMap.size=%d", ()->rec2idxListMap.size());
			iter = rec2idxListMap.entrySet().iterator();
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
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<>();
			int countUpperBound = tokenCounter.sumBounds();
			for ( int token : candTokenSet ) {
				tokenCounter.clear();
				PositionInvList invList = index.getInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%s (%d), len(invList)=%d", ()->Record.tokenIndex.getToken(token), ()->token, ()->invList==null?0:invList.size());
//				Log.log.trace("invList=%s", ()->invList);
				if ( invList != null ) {
//					Log.log.trace("TextSideFilter.getCommonTokenIdxLists: token=%s, invList.size=%d", ()->Record.tokenIndex.getToken(token), ()->invList.size());

					if ( !useCF || countUpperBound >= minCount ) {
						statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.scan");
						for ( int i=0; i<invList.size(); ++i ) {
							int ridx = invList.getIdx(i);
							int pos = invList.getPos(i);
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
					}
					else {
						statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
						for ( int ridx : rec2idxListMap.keySet() ) {
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( pair.nToken + countUpperBound >= minCount ) {
								int idx = Util.binarySearch(invList, ridx);
								if ( idx >= 0 ) {
									while ( idx < invList.size() && invList.getIdx(idx) == ridx ) {
										if ( tokenCounter.tryIncrement(ridx, token) ) {
											pair.nToken += 1;
										}
										pair.prefixList.add(invList.getPos(idx));
										pair.suffixTokenList.add(new IntPair(invList.getPos(idx), token));
										idx += 1;
									}
								}
							}
						}
						statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
					}
				} // end if invList

				PositionTrInvList transInvList = index.getTransInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%s (%d), len(transInvList)=%d", ()->Record.tokenIndex.getToken(token), ()->token, ()->transInvList==null?0:transInvList.size());
//				Log.log.trace("transInvList=%s", transInvList);
				if ( transInvList != null ) {
//					Log.log.trace("TextSideFilter.getCommonTokenIdxLists: token=%s, transInvList.size=%d", ()->Record.tokenIndex.getToken(token), ()->transInvList.size());

					if ( !useCF || countUpperBound >= minCount ) {
						statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.scan");
						for ( int i=0; i<transInvList.size(); ++i ) {
							int ridx = transInvList.getIdx(i);
							int left = transInvList.getLeft(i);
							int right = transInvList.getRight(i);
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
					}
					else {
						statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
						for ( int ridx : rec2idxListMap.keySet() ) {
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( pair.nToken + countUpperBound >= minCount ) {
								int idx = Util.binarySearch(transInvList, ridx);
								if ( idx >= 0 ) {
									while ( idx < transInvList.size() && transInvList.getIdx(idx) == ridx ) {
										if ( tokenCounter.tryIncrement(ridx, token) ) {
											pair.nToken += 1;
										}
										pair.prefixList.add(transInvList.getLeft(idx));
										pair.suffixTokenList.add(new IntPair(transInvList.getRight(idx), token));
										idx += 1;
									}
								}
							}
						}
						statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists.binarySearch");
					}
				} // end if transInvList
				countUpperBound -= tokenCounter.getMax(token);
			} // end for token

//			Log.log.trace("TextSideFilter: rec2idxListMap.size=%d", ()->rec2idxListMap.size());
//			Log.log.trace("TextSideFilter: rec2idxListMap.length=%d", ()->rec2idxListMap.values().stream().mapToInt(x->x.prefixList.size()).sum());
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

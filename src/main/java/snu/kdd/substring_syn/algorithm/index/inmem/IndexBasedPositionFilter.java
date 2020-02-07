package snu.kdd.substring_syn.algorithm.index.inmem;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map.Entry;

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
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordWithEndpoints;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.MaxBoundTokenCounter;
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
	public ObjectList<Record> getCandRecordsQuerySide( Record query ) {
		QuerySideFilter filter = new QuerySideFilter(query);
		return filter.run();
	}
	
	private class QuerySideFilter {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		
		public QuerySideFilter( Record query ) {
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
		}
		
		public ObjectList<Record> run() {
//			Log.log.trace("PositionalIndexBasedFilter.querySideFilter(%d)", ()->query.getID());
			ObjectList<Record> candRecordSet = new ObjectArrayList<>();
//			Log.log.trace("minCount=%d", ()->minCount);
			statContainer.startWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			statContainer.stopWatch("Time_QS_IndexFilter.getCommonTokenIdxLists");
			Iterator<Entry<Integer, PosListPair>> iter = rec2idxListMap.entrySet().stream().sorted((x,y)->Integer.compare(x.getKey(), y.getKey())).iterator();
			while ( iter.hasNext() ) {
				Entry<Integer, PosListPair> entry = iter.next();
				if ( useCF && entry.getValue().nToken < minCount ) continue;
				int ridx = entry.getKey();
				statContainer.startWatch("Time_QS_IndexFilter.getRecord");
				Record rec = dataset.getRawRecord(ridx);
				statContainer.stopWatch("Time_QS_IndexFilter.getRecord");
				IntList idxList = entry.getValue().idxList;
				statContainer.startWatch("Time_QS_IndexFilter.idxList.sort");
				idxList.sort(Integer::compare);
				statContainer.stopWatch("Time_QS_IndexFilter.idxList.sort");
//				Log.log.trace("idxList=%s", ()->idxList);
//				Log.log.trace("visualizeCandRecord(%d): %s", ()->rec.getID(), ()->visualizeCandRecord(rec, idxList));
				statContainer.startWatch("Time_QS_IndexFilter.pruneSingleRecord");
				ObjectList<Record> segmentList =  pruneSingleRecord(rec, idxList);
				statContainer.stopWatch("Time_QS_IndexFilter.pruneSingleRecord");
				statContainer.startWatch("Time_QS_IndexFilter.candRecordSet.addAll");
				candRecordSet.addAll(segmentList);
				statContainer.stopWatch("Time_QS_IndexFilter.candRecordSet.addAll");
			}
			return candRecordSet;
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists() {
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<PosListPair>();
			int countUpperBound = tokenCounter.sumBounds();
			for ( int i=0; i<candTokenSet.size(); ++i ) {
				int token = candTokenSet.getInt(i);
				int nMax = tokenCounter.getMax(token);
				Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
				tokenCounter.clear();
				ObjectList<InvListEntry> invList = index.getInvList(token);
				if ( invList != null ) {
//					Log.log.trace("QuerySideFilter.getCommonTokenIdxLists: token=%s, invList.size=%d", ()->Record.tokenIndex.getToken(token), ()->invList.size());
					if ( !useCF || countUpperBound >= minCount ) {
						// there is a chance that a record not seen until now can have at least minCount common tokens.
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
					else {
						// all unseen records cannot be the answer by the count filtering so we ignore them.
						// we use the binary search to update the count of only the records in rec2idxListMap.
						for ( int ridx : rec2idxListMap.keySet() ) {
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( pair.nToken + countUpperBound >= minCount ) {
								int idx = Util.binarySearch(invList,  new InvListEntry(ridx, 0), (x,y)->Integer.compare(x.ridx, y.ridx));
								if ( idx >= 0 ) {
									while ( idx < invList.size() && invList.get(idx).ridx == ridx ) {
										if ( counter.get(ridx) < nMax ) {
											counter.addTo(ridx, 1);
											pair.nToken += 1;
										}
										pair.idxList.add(invList.get(idx).pos);
										idx += 1;
									}
								}
							}
						}
					}
				}
				countUpperBound -= nMax;
			}
			return rec2idxListMap;
		}
		
		private ObjectList<Record> pruneSingleRecord( Record rec, IntList idxList ) {
			ObjectList<MergedRange> segmentRangeList = findSegments(rec, idxList, theta);
//			Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
			ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList, idxList );
			return segmentList;
		}

		private ObjectList<MergedRange> findSegments( Record rec, IntList idxList, double theta ) {
			int m = idxList.size();
			ObjectList<MergedRange> rangeList = new ObjectArrayList<>();
			for ( int i=0; i<m; ++i ) {
				int sidx = idxList.get(i);
				tokenCounter.clear();
				MergedRange mrange = new MergedRange(sidx);
				for ( int j=i; j<m; ++j ) {
					int eidx = idxList.get(j);
					int token = rec.getToken(eidx);
					tokenCounter.tryIncrement(token);
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
				if ( mrange.eidxList.size() > 0 ) rangeList.add(mrange);
			}
//			Log.log.trace("rangeList=%s", ()->rangeList);
			if ( rangeList.size() == 0 ) return null;
			return rangeList;
		}
		
		private ObjectList<Record> splitRecord( Record rec, ObjectList<MergedRange> segmentRangeList, IntList idxList ) {
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( MergedRange mrange : segmentRangeList ) {
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
	public ObjectList<Record> getCandRecordsTextSide( Record query ) {
		TextSideFilter filter = new TextSideFilter(query);
		return filter.run();
	}
	
	private class TextSideFilter {

		final Record query;
		final IntList candTokenSet; // unique, sorted
		final MaxBoundTokenCounter tokenCounter;
		final int minCount;
		
		public TextSideFilter( Record query ) {
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
		}
		
		public ObjectList<Record> run() {
//			Log.log.trace("PositionalIndexBasedFilter.textSideFilter(%d)", ()->query.getID());
			ObjectList<Record> candRecordSet = new ObjectArrayList<>();
//			Log.log.trace("minCount=%d", ()->minCount);
			statContainer.startWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
			Int2ObjectMap<PosListPair> rec2idxListMap = getCommonTokenIdxLists();
			statContainer.stopWatch("Time_TS_IndexFilter.getCommonTokenIdxLists");
//			Log.log.trace("rec2idxListMap.size=%d", ()->rec2idxListMap.size());
			Iterator<Entry<Integer, PosListPair>> iter = rec2idxListMap.entrySet().stream().sorted((x,y)->Integer.compare(x.getKey(), y.getKey())).iterator();
			while ( iter.hasNext() ) {
				Entry<Integer, PosListPair> e = iter.next();
				if ( useCF && e.getValue().nToken < minCount ) continue;
				int ridx = e.getKey();
//				Log.log.trace("ridx=%d", ridx);
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
				Record fullRec = dataset.getRecord(ridx);
				statContainer.stopWatch("Time_TS_IndexFilter.getRecord");
				statContainer.startWatch("Time_TS_IndexFilter.getSubrecord");
				Record rec = (new Subrecord(fullRec, minPrefixIdx, maxSuffixIdx+1)).toRecord();
				statContainer.stopWatch("Time_TS_IndexFilter.getSubrecord");
				addToIntList(prefixIdxList, -minPrefixIdx);
				addToIntList(suffixIdxList, -minPrefixIdx);
//				statContainer.startWatch("Time_TS_IndexFilter.preprocess");
//				statContainer.stopWatch("Time_TS_IndexFilter.preprocess");
				double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
//				statContainer.startWatch("Time_TS_IndexFilter.transLen");
//				TransLenCalculator transLen = new TransLenCalculator(null, rec, modifiedTheta);
//				statContainer.stopWatch("Time_TS_IndexFilter.transLen");
				statContainer.startWatch("Time_TS_IndexFilter.findSegmentRanges");
				ObjectList<MergedRange> segmentRangeList = findSegmentRanges(rec, prefixIdxList, suffixIdxList, tokenList, modifiedTheta);
				statContainer.stopWatch("Time_TS_IndexFilter.findSegmentRanges");
//				Log.log.trace("segmentRangeList=%s", ()->segmentRangeList);
				statContainer.startWatch("Time_TS_IndexFilter.splitRecord");
				ObjectList<Record> segmentList = splitRecord(rec, segmentRangeList);
				statContainer.stopWatch("Time_TS_IndexFilter.splitRecord");
				statContainer.startWatch("Time_TS_IndexFilter.addAllCands");
				candRecordSet.addAll(segmentList);
				statContainer.stopWatch("Time_TS_IndexFilter.addAllCands");
			}
			return candRecordSet;
		}
		
		private Int2ObjectMap<PosListPair> getCommonTokenIdxLists() {
			Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<>();
			int countUpperBound = tokenCounter.sumBounds();
			for ( int token : candTokenSet ) {
				int nMax = tokenCounter.getMax(token);
				Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
				ObjectList<InvListEntry> invList = index.getInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%d, len(invList)=%d", ()->token, ()->invList==null?0:invList.size());
				if ( invList != null ) {
//					Log.log.trace("TextSideFilter.getCommonTokenIdxLists: token=%s, invList.size=%d", ()->Record.tokenIndex.getToken(token), ()->invList.size());

					if ( !useCF || countUpperBound >= minCount ) {
						for ( InvListEntry e : invList ) {
							
							//statContainer.startWatch("Time_TS_getCommon.rec2idxListMap1");
							if ( !rec2idxListMap.containsKey(e.ridx) ) rec2idxListMap.put(e.ridx, new PosListPair());
							//statContainer.stopWatch("Time_TS_getCommon.rec2idxListMap1");
							PosListPair pair = rec2idxListMap.get(e.ridx);
							//statContainer.startWatch("Time_TS_getCommon.counter_getAndAdd1");
							if ( counter.get(e.ridx) < nMax ) {
								//statContainer.startWatch("Time_TS_getCommon.counter_addTo1");
								counter.addTo(e.ridx, 1);
								//statContainer.stopWatch("Time_TS_getCommon.counter_addTo1");
								pair.nToken += 1;
							}
							//statContainer.stopWatch("Time_TS_getCommon.counter_getAndAdd1");
							//statContainer.startWatch("Time_TS_getCommon.prefixListAdd1");
							pair.prefixList.add(e.pos);
							//statContainer.stopWatch("Time_TS_getCommon.prefixListAdd1");
							//statContainer.startWatch("Time_TS_getCommon.suffixTokenListAdd1");
							pair.suffixTokenList.add(new IntPair(e.pos, token));
							//statContainer.stopWatch("Time_TS_getCommon.suffixTokenListAdd1");
						}
					}
					else {
						for ( int ridx : rec2idxListMap.keySet() ) {
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( pair.nToken + countUpperBound >= minCount ) {
								int idx = Util.binarySearch(invList,  new InvListEntry(ridx, 0), (x,y)->Integer.compare(x.ridx, y.ridx));
								if ( idx >= 0 ) {
									while ( idx < invList.size() && invList.get(idx).ridx == ridx ) {
										if ( counter.get(ridx) < nMax ) {
											counter.addTo(ridx, 1);
											pair.nToken += 1;
										}
										InvListEntry e = invList.get(idx);
										pair.prefixList.add(e.pos);
										pair.suffixTokenList.add(new IntPair(e.pos, token));
										idx += 1;
									}
								}
							}
						}
					}
				} // end if invList

				ObjectList<TransInvListEntry> transInvList = index.getTransInvList(token);
//				Log.log.trace("getCommonTokenIdxLists\ttoken=%d, len(transInvList)=%d", ()->token, ()->transInvList==null?0:transInvList.size());
				if ( transInvList != null ) {
//					Log.log.trace("TextSideFilter.getCommonTokenIdxLists: token=%s, transInvList.size=%d", ()->Record.tokenIndex.getToken(token), ()->transInvList.size());

					if ( !useCF || countUpperBound >= minCount ) {
						for ( TransInvListEntry e : transInvList ) {
							//statContainer.startWatch("Time_TS_getCommon.rec2idxListMap2");
							if ( !rec2idxListMap.containsKey(e.ridx) ) rec2idxListMap.put(e.ridx, new PosListPair());
							//statContainer.stopWatch("Time_TS_getCommon.rec2idxListMap2");
							PosListPair pair = rec2idxListMap.get(e.ridx);
							//statContainer.startWatch("Time_TS_getCommon.counter_getAndAdd2");
							if ( counter.get(e.ridx) < nMax ) {
								//statContainer.startWatch("Time_TS_getCommon.counter_addTo2");
								counter.addTo(e.ridx, 1);
								//statContainer.stopWatch("Time_TS_getCommon.counter_addTo2");
								pair.nToken += 1;
							}
							//statContainer.stopWatch("Time_TS_getCommon.counter_getAndAdd2");
							//statContainer.startWatch("Time_TS_getCommon.prefixListAdd2");
							pair.prefixList.add(e.left);
							//statContainer.stopWatch("Time_TS_getCommon.prefixListAdd2");
							//statContainer.startWatch("Time_TS_getCommon.suffixTokenListAdd2");
							pair.suffixTokenList.add(new IntPair(e.right, token));
							//statContainer.stopWatch("Time_TS_getCommon.suffixTokenListAdd2");
						}
					}
					else {
						for ( int ridx : rec2idxListMap.keySet() ) {
							PosListPair pair = rec2idxListMap.get(ridx);
							if ( pair.nToken + countUpperBound >= minCount ) {
								int idx = Util.binarySearch(transInvList,  new TransInvListEntry(ridx, 0, 0), (x,y)->Integer.compare(x.ridx, y.ridx));
								if ( idx >= 0 ) {
									while ( idx < transInvList.size() && transInvList.get(idx).ridx == ridx ) {
										if ( counter.get(ridx) < nMax ) {
											counter.addTo(ridx, 1);
											pair.nToken += 1;
										}
										TransInvListEntry e = transInvList.get(idx);
										pair.prefixList.add(e.left);
										pair.suffixTokenList.add(new IntPair(e.right, token));
										idx += 1;
									}
								}
							}
						}
					}
				} // end if transInvList
				countUpperBound -= nMax;
			} // end for token
			return rec2idxListMap;
		}
		
		private void addToIntList( IntList list, int c ) {
			for ( int i=0; i<list.size(); ++i ) list.set(i, list.get(i)+c);
		}

		private ObjectList<MergedRange> findSegmentRanges( Record rec, IntList prefixIdxList, IntList suffixIdxList, IntList tokenList, double theta ) {
//			System.out.println("minPrefixIdx: "+minPrefixIdx+", maxSuffixIdx: "+maxSuffixIdx);
			ObjectList<MergedRange> rangeList = new ObjectArrayList<>();
			for ( int i=0; i<prefixIdxList.size(); ++i ) {
				int sidx = prefixIdxList.get(i);
				MergedRange mrange = new MergedRange(sidx);
				TransLenLazyCalculator transLen = new TransLenLazyCalculator(statContainer, rec, sidx, rec.size()-sidx, theta);
				tokenCounter.clear();
				for ( int j=0; j<suffixIdxList.size(); ++j ) {
					int eidx = suffixIdxList.get(j);
					if ( eidx < sidx ) continue;
					int token = tokenList.get(j);
					tokenCounter.tryIncrement(token);
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
				if ( mrange.eidxList.size() > 0 ) rangeList.add(mrange);
			}
//			Log.log.trace("rangeList=%s", ()->rangeList);
			if ( rangeList.size() == 0 ) return null;
			return rangeList;
		}

		private ObjectList<Record> splitRecord( Record rec, ObjectList<MergedRange> segmentRangeList ) {
			ObjectList<Record> segmentList = new ObjectArrayList<>();
			if ( segmentRangeList != null ) {
				for ( MergedRange mrange : segmentRangeList ) {
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

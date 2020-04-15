package snu.kdd.substring_syn.algorithm.index.inmem;

import java.math.BigInteger;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedNaiveInvertedIndex;
import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Int2IntBinaryHeap;
import snu.kdd.substring_syn.utils.MaxBoundTokenCounter;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedNaiveFilter extends AbstractIndexBasedFilter {

	protected final DiskBasedNaiveInvertedIndex index;
	
	public IndexBasedNaiveFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(dataset, theta, statContainer);
		index = new DiskBasedNaiveInvertedIndex(dataset.getIndexedList());
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
	public IntIterable querySideFilter( Record query ) {
		statContainer.startWatch(Stat.Time_QS_IndexFilter);
		int minCount = getQuerySideMinCount(query);
//		Log.log.trace("query.size()=%d, query.getTransSetLB()=%d", ()->query.size(), ()->query.getTransSetLB());
		IntList candTokenList = new IntArrayList();
		for ( Rule r : query.getApplicableRuleIterable() ) {
			for ( int token : r.getRhs() ) candTokenList.add(token);
		}
		IntList candTokenSet = new IntArrayList(candTokenList.stream().sorted().distinct().iterator());

		MaxBoundTokenCounter tokenCounter = new MaxBoundTokenCounter(candTokenList);
		Int2ObjectMap<NaiveInvList> tok2listMap = new Int2ObjectOpenHashMap<>();
		Int2IntBinaryHeap heap = new Int2IntBinaryHeap();
		for ( int token : candTokenSet ) {
			NaiveInvList list = index.getInvList(token);
			if ( list != null ) {
				list.init();
				tok2listMap.put(token, list);
				heap.insert(list.getIdx(), token);
			}
		}
		statContainer.stopWatch(Stat.Time_QS_IndexFilter);
		
		Iterator<Integer> iter = new Iterator<Integer>() {
			
			int nextRidx = findNext();
			
			@Override
			public Integer next() {
				statContainer.startWatch(Stat.Time_QS_IndexFilter);
				int ridx = nextRidx;
				nextRidx = findNext();
				statContainer.stopWatch(Stat.Time_QS_IndexFilter);
				return ridx;
			}

			private int findNext() {
				while ( !heap.isEmpty() ) {
					tokenCounter.clear();
					int nextRidx = extractHead();
					while ( !heap.isEmpty() && heap.peekKey() == nextRidx ) extractHead();
					if ( tokenCounter.sum() >= minCount ) return nextRidx;
				}
				return -1;
			}

			private int extractHead() {
				IntPair head = heap.poll();
				int token = head.i2;
				tokenCounter.tryIncrement(token);
				getNextFromList(token);
				return head.i1;
			}

			private void getNextFromList(int token) {
				NaiveInvList list = tok2listMap.get(token);
				list.next();
				if ( list.hasNext() ) heap.insert(list.getIdx(), token);
			}
			
			@Override
			public boolean hasNext() {
				return nextRidx != -1;
			}
		};

		return new IntIterable() {

			@Override
			public IntIterator iterator() {
				return IntIterators.asIntIterator(iter);
			}
		};
	}
	
	protected int getQuerySideMinCount(Record query) {
		return 0;
	}
	
	@Override
	public IntIterable textSideFilter( Record query ) {
		statContainer.startWatch(Stat.Time_TS_IndexFilter);
		int minCount = getTextSideMinCount(query);
		IntList candTokenSet = new IntArrayList(query.getTokens().stream().sorted().distinct().iterator());

		MaxBoundTokenCounter tokenCounter = new MaxBoundTokenCounter(query.getTokenList());
		Int2ObjectMap<NaiveInvList> tok2listMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<NaiveInvList> tok2trlistMap = new Int2ObjectOpenHashMap<>();
		Int2IntBinaryHeap heap = new Int2IntBinaryHeap();
		for ( int token : candTokenSet ) {
			NaiveInvList list = index.getInvList(token);
			if ( list != null ) {
				list.init();
				tok2listMap.put(token, list);
				heap.insert(list.getIdx(), token);
			}
			NaiveInvList trlist = index.getTransInvList(token);
			if ( trlist != null ) {
				trlist.init();
				tok2trlistMap.put(token, trlist);
				heap.insert(trlist.getIdx(), -token-1); // assume token >= 0
			}
		}
		statContainer.stopWatch(Stat.Time_TS_IndexFilter);
		
		Iterator<Integer> iter = new Iterator<Integer>() {

			int nextRidx = findNext();
			
			@Override
			public Integer next() {
				statContainer.startWatch(Stat.Time_TS_IndexFilter);
				int ridx = nextRidx;
				nextRidx = findNext();
				statContainer.stopWatch(Stat.Time_TS_IndexFilter);
				return ridx;
			}

			private int findNext() {
				while ( !heap.isEmpty() ) {
					tokenCounter.clear();
					int nextRidx = extractHead();
					while ( !heap.isEmpty() && heap.peekKey() == nextRidx ) extractHead();
					if ( tokenCounter.sum() >= minCount ) return nextRidx;
				}
				return -1;
			}
			
			private int extractHead() {
				IntPair head = heap.poll();
				int token = head.i2;
				
				if ( token >= 0 ) getNextFromList(token);
				else {
					token = -token-1;
					getNextFromTrList(token);
				}
				tokenCounter.tryIncrement(token);
				return head.i1;
			}

			private void getNextFromList(int token) {
				NaiveInvList list = tok2listMap.get(token);
				list.next();
				if ( list.hasNext() ) heap.insert(list.getIdx(), token);
			}

			private void getNextFromTrList(int token) {
				NaiveInvList list = tok2trlistMap.get(token);
				list.next();
				if ( list.hasNext() ) heap.insert(list.getIdx(), -token-1);
			}

			@Override
			public boolean hasNext() {
				return nextRidx != -1;
			}
		};
		
		return new IntIterable() {

			@Override
			public IntIterator iterator() {
				return IntIterators.asIntIterator(iter);
			}
		};
	}

	protected int getTextSideMinCount(Record query) {
		return 0;
	}
	
	@Override
	public BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage();
	}
}

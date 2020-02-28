package snu.kdd.substring_syn.index;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalInvertedIndex;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.MaxBoundTokenCounter;
import snu.kdd.substring_syn.utils.StatContainer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IndexBasedPositionFilterTest {
	
	@BeforeClass
	public static void init() {
		StatContainer.global = new StatContainer();
	}

	@Test
	public void testRecordIdPosMonotonicity() throws IOException {
		DatasetParam param = new DatasetParam("AMAZON", "10000", "107836", "5", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		DiskBasedPositionalInvertedIndex index = new DiskBasedPositionalInvertedIndex(dataset.getIndexedList());
//		filter = new IndexBasedPositionFilter(dataset, theta, false, statContainer);
		
		for ( int token=0; token<=Record.tokenIndex.getMaxID(); ++token ) {
			PositionInvList list = index.getInvList(token);
			if ( list == null ) continue;
			int id0 = -1;
			int pos0 = -1;
			int id1 = list.getId(0);
			int pos1 = list.getPos(0);
			for ( int i=1; i<list.size(); ++i ) {
				id0 = id1;
				pos0 = pos1;
				id1 = list.getId(i);
				pos1 = list.getPos(i);
				assertTrue( id0 <= id1 );
				if ( id0 == id1 ) {
					assertTrue( pos0 <= pos1 );
				}
			}
		}

		for ( int token=0; token<=Record.tokenIndex.getMaxID(); ++token ) {
			PositionTrInvList list = index.getTransInvList(token);
			if ( list == null ) continue;
			int id0 = -1;
			int left0 = -1;
			int id1 = list.getId(0);
			int left1 = list.getLeft(0);
			for ( int i=1; i<list.size(); ++i ) {
				id0 = id1;
				left0 = left1;
				id1 = list.getId(i);
				left1 = list.getLeft(i);
				assertTrue( id0 <= id1 );
				if ( id0 == id1 ) {
					assertTrue( left0 <= left1 );
				}
			}
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testEfficiency() throws IOException {
		/*
		 * 1046.1759
		 */
		DatasetParam param = new DatasetParam("AMAZON", "100000", "107836", "5", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		DiskBasedPositionalInvertedIndex index = new DiskBasedPositionalInvertedIndex(dataset.getIndexedList());
		long ts;
		ts = System.nanoTime();
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			Int2ObjectMap<PosListPair> rec2idxListMap0 = getCommonTokenIdxLists0(query, index);
//			IntArrayList intList0 = toIntList(rec2idxListMap0);
		}
		System.out.println((System.nanoTime()-ts)/1e6);
	}

	private Int2ObjectMap<PosListPair> getCommonTokenIdxLists0( Record query, DiskBasedPositionalInvertedIndex index ) {

		IntOpenHashSet candTokenSet = new IntOpenHashSet();
		IntArrayList candTokenList = new IntArrayList();
		for ( Rule r : query.getApplicableRuleIterable() ) {
			for ( int token : r.getRhs() ) {
				candTokenSet.add(token);
				candTokenList.add(token);
			}
		}

		MaxBoundTokenCounter tokenCounter = new MaxBoundTokenCounter(candTokenList);
		Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<PosListPair>();
		for ( int token : candTokenSet ) {
			int nMax = tokenCounter.getMax(token);
			Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
			tokenCounter.clear();
			PositionInvList invList = index.getInvList(token);
			if ( invList == null ) continue; 
			for ( int i=0; i<invList.size(); ++i ) {
				int ridx = invList.getId(i);
				int pos = invList.getPos(i);
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

	@SuppressWarnings("unused")
	private Int2ObjectMap<PosListPair> getCommonTokenIdxLists1( Record query, DiskBasedPositionalInvertedIndex index ) {

		IntOpenHashSet candTokenSet = new IntOpenHashSet();
		IntArrayList candTokenList = new IntArrayList();
		for ( Rule r : query.getApplicableRuleIterable() ) {
			for ( int token : r.getRhs() ) {
				candTokenSet.add(token);
				candTokenList.add(token);
			}
		}

		MaxBoundTokenCounter tokenCounter = new MaxBoundTokenCounter(candTokenList);
		Int2ObjectMap<PosListPair> rec2idxListMap = new Int2ObjectOpenHashMap<PosListPair>();
		for ( int token : candTokenSet ) {
			int nMax = tokenCounter.getMax(token);
			Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
			tokenCounter.clear();
			PositionInvList invList = index.getInvList(token);
			if ( invList == null ) continue; 
			for ( int i=0; i<invList.size; ++i ) {
				int ridx = invList.getId(i);
				int pos = invList.getPos(i);
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

	private class PosListPair {
		int nToken = 0;
		IntList idxList = new IntArrayList();
	}
	
	@SuppressWarnings("unused")
	private IntArrayList toIntList(Int2ObjectMap<PosListPair> map) {
		IntArrayList list = new IntArrayList();
		Iterator<Integer> iter = map.keySet().stream().iterator();
		while ( iter.hasNext() ) {
			int key = iter.next();
			PosListPair val = map.get(key);
			list.add(key);
			list.add(val.nToken);
			list.addAll(val.idxList);
		}
		return list;
	}

}

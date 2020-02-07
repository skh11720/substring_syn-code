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
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalIndexInterface.InvListEntry;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalIndexInterface.TransInvListEntry;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalInvertedIndex;
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
			ObjectList<InvListEntry> list = index.getInvList(token);
			if ( list == null ) continue;
			InvListEntry e0 = null;
			InvListEntry e1 = list.get(0);
			for ( int i=1; i<list.size(); ++i ) {
				e0 = e1;
				e1 = list.get(i);
				assertTrue( e0.ridx <= e1.ridx );
				if ( e0.ridx == e1.ridx ) {
					assertTrue( e0.pos <= e1.pos );
				}
			}
		}

		for ( int token=0; token<=Record.tokenIndex.getMaxID(); ++token ) {
			ObjectList<TransInvListEntry> list = index.getTransInvList(token);
			if ( list == null ) continue;
			TransInvListEntry e0 = null;
			TransInvListEntry e1 = list.get(0);
			for ( int i=1; i<list.size(); ++i ) {
				e0 = e1;
				e1 = list.get(i);
				assertTrue( e0.ridx <= e1.ridx );
				if ( e0.ridx == e1.ridx ) {
					assertTrue( e0.left <= e1.left );
				}
			}
		}
	}

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

	private class PosListPair {
		int nToken = 0;
		IntList idxList = new IntArrayList();
	}
	
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

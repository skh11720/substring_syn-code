package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedNaiveInvertedIndex;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class IndexBasedCountFilter extends AbstractIndexBasedFilter {

	protected final DiskBasedNaiveInvertedIndex index;
	
	public IndexBasedCountFilter( Dataset dataset, double theta, StatContainer statContainer ) {
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
	public ObjectList<Record> querySideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.getTransSetLB());
		Log.log.trace("query.size()=%d, query.getTransSetLB()=%d", ()->query.size(), ()->query.getTransSetLB());
		Int2IntOpenHashMap commonTokenCounter = new Int2IntOpenHashMap();
		IntSet candTokenSet = query.getCandTokenSet();
		Int2IntMap tokenMaxCountMap = Util.getCounter(candTokenSet);
		for ( int token : candTokenSet ) {
			int nMax = tokenMaxCountMap.get(token);
			Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
			ObjectList<Integer> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( int ridx : invList ) {
					if ( counter.get(ridx) < nMax ) {
						counter.addTo(ridx, 1);
						commonTokenCounter.addTo(ridx, 1);
					}
				}
			}
		}
		
		ObjectList<Record> candRecordSet = new ObjectArrayList<>(pruneRecordsByCount(commonTokenCounter, minCount));
//		visualizeCandRecords(candTokenSet, candRecordSet, counter);

		return candRecordSet;
	}
	
	@Override
	public ObjectList<Record> textSideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.size());
		Int2IntOpenHashMap commonTokenCounter = new Int2IntOpenHashMap();
		Int2IntMap tokenMaxCountMap = Util.getCounter(query.getTokenArray());
		for ( int token : query.getTokens() ) {
			int nMax = tokenMaxCountMap.get(token);
			Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
			ObjectList<Integer> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( int ridx : invList ) {
					if ( counter.get(ridx) < nMax ) {
						counter.addTo(ridx, 1);
						commonTokenCounter.addTo(ridx, 1);
					}
				}
			}
			ObjectList<Integer> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) {
				for ( int ridx : transInvList ) {
					if ( counter.get(ridx) < nMax ) {
						counter.addTo(ridx, 1);
						commonTokenCounter.addTo(ridx, 1);
					}
				}
			}
		}

		ObjectList<Record> candRecordSet = pruneRecordsByCount(commonTokenCounter, minCount);
		return candRecordSet;
	}
	
	private ObjectList<Record> pruneRecordsByCount( Int2IntMap counter, int minCount ) {
		ObjectList<Record> candRecordSet = new ObjectArrayList<>();
		for ( Int2IntMap.Entry entry : counter.int2IntEntrySet() ) {
			int ridx = entry.getIntKey();
			int count = entry.getIntValue();
			if ( count >= minCount ) candRecordSet.add(dataset.getRecord(ridx));
		}
		return candRecordSet;
	}
}

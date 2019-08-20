package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedCountFilter extends AbstractIndexBasedFilter {

	protected final NaiveInvertedIndex index;
	protected final boolean useCountFilter = true;
	
	public IndexBasedCountFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(theta, statContainer);
		index = new NaiveInvertedIndex(dataset);
	}

	@Override
	public long invListSize() {
		long size = 0;
		for ( ObjectList<Record> list : index.invList.values() ) size += list.size();
		return size;
	}
	
	@Override
	public long transInvListSize() {
		long size = 0;
		for ( ObjectList<Record> list : index.transInvList.values() ) size += list.size();
		return size;
	}
	
	@Override
	public ObjectSet<Record> querySideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.getTransSetLB());
		Log.log.trace("query.size()=%d, query.getTransSetLB()=%d", ()->query.size(), ()->query.getTransSetLB());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<Record> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( Record rec : invList ) counter.addTo(rec, 1);
			}
		}
		
		ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>(pruneRecordsByCount(counter, minCount));
//		visualizeCandRecords(candTokenSet, candRecordSet, counter);

		return candRecordSet;
	}
	
	@Override
	public ObjectSet<Record> textSideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.size());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		for ( int token : query.getTokens() ) {
			ObjectList<Record> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( Record rec : invList ) counter.addTo(rec, 1);
			}
			ObjectList<Record> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) {
				for ( Record rec : transInvList ) counter.addTo(rec, 1);
			}
		}

		ObjectSet<Record> candRecordSet = pruneRecordsByCount(counter, minCount);
		return candRecordSet;
	}
	
	private ObjectSet<Record> pruneRecordsByCount( Object2IntMap<Record> counter, int minCount ) {
		if ( !useCountFilter ) return new ObjectOpenHashSet<Record>(counter.keySet());
		ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
		for ( Object2IntMap.Entry<Record> entry : counter.object2IntEntrySet() ) {
			Record rec = entry.getKey();
			int count = entry.getIntValue();
			if ( count >= minCount ) candRecordSet.add(rec);
		}
		return candRecordSet;
	}
}

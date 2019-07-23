package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;

public class NaiveIndexBasedFilter extends AbstractIndexBasedFilter {

	protected final NaiveInvertedIndex index;
	protected final boolean useCountFilter = true;
	
	public NaiveIndexBasedFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(theta, statContainer);
		index = new NaiveInvertedIndex(dataset);
	}
	
	@Override
	public ObjectSet<RecordInterface> querySideFilter( Record query ) {
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

		statContainer.startWatch("Time_QS_IndexCountFilter");
		ObjectSet<RecordInterface> candRecordSet = new ObjectOpenHashSet<>(pruneRecordsByCount(counter, minCount));
		statContainer.stopWatch("Time_QS_IndexCountFilter");
		statContainer.addCount("Num_QS_IndexCountFilter", candRecordSet.size());
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

		statContainer.startWatch("Time_TS_IndexCountFilter");
		ObjectSet<Record> candRecordSet = pruneRecordsByCount(counter, minCount);
		statContainer.stopWatch("Time_TS_IndexCountFilter");
		statContainer.addCount("Num_TS_IndexCountFilter", candRecordSet.size());
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

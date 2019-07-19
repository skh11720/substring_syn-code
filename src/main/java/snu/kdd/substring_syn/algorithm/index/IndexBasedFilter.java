package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedFilter {

	protected final InvertedIndex index;
	protected final double theta;
	protected final StatContainer statContainer;
	protected final boolean useCountFilter = true;
	
	public IndexBasedFilter( InvertedIndex index, double theta, StatContainer statContainer ) {
		this.index = index;
		this.theta = theta;
		this.statContainer = statContainer;
	}
	
	public ObjectSet<Record> querySideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.getTransSetLB());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<IndexEntry> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( IndexEntry e : invList ) counter.addTo(e.rec, 1);
			}
		}

		statContainer.startWatch("Time_QS_IndexCountFilter");
		ObjectSet<Record> candRecordSet = pruneRecordsByCount(counter, minCount);
		statContainer.stopWatch("Time_QS_IndexCountFilter");
		statContainer.addCount("Num_QS_IndexCountFilter", candRecordSet.size());
		visualizeCandRecords(candTokenSet, candRecordSet, counter);

		return candRecordSet;
	}
	
	public ObjectSet<Record> textSideFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.size());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		for ( int token : query.getTokens() ) {
			ObjectList<IndexEntry> invList = index.getInvList(token);
			if ( invList != null ) {
				for ( IndexEntry e : invList ) counter.addTo(e.rec, 1);
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
	
	private void visualizeCandRecords( IntSet candTokenSet, ObjectSet<Record> candRecordList, Object2IntOpenHashMap<Record> counter ) {
		for ( Record rec : candRecordList ) {
			int[] tokens = rec.getTokenArray();
			StringBuilder strbld = new StringBuilder();
			for ( int token : tokens ) {
				if ( candTokenSet.contains(token) ) strbld.append("O");
				else strbld.append('-');
			}
			System.out.println(counter.getInt(rec)+"\t"+strbld.toString());
		}
	}
}

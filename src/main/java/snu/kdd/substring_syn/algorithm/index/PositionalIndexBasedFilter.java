package snu.kdd.substring_syn.algorithm.index;

import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.index.PositionalInvertedIndex.IndexEntry;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class PositionalIndexBasedFilter {

	protected final PositionalInvertedIndex index;
	protected final double theta;
	protected final StatContainer statContainer;
	protected final boolean useCountFilter = true;
	
	public PositionalIndexBasedFilter( PositionalInvertedIndex index, double theta, StatContainer statContainer ) {
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
//		visualizeCandRecords(candTokenSet, candRecordSet, counter);

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
		ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
		for ( Object2IntMap.Entry<Record> entry : counter.object2IntEntrySet() ) {
			Record rec = entry.getKey();
			int count = entry.getIntValue();
			if ( !useCountFilter || count >= minCount ) candRecordSet.add(rec);
		}
		return candRecordSet;
	}
	
	private void pruneRecords( Record query ) {
		int minCount = (int)Math.ceil(theta*query.size());
		Object2ObjectMap<Record, IntList> recPosListMap = new Object2ObjectOpenHashMap<Record, IntList>();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<IndexEntry> invList = index.getInvList(token);
			if ( invList == null ) continue; 
			for ( IndexEntry e : invList ) {
				Record rec = e.rec;
				int pos = e.pos;
				if ( !recPosListMap.containsKey(rec) ) recPosListMap.put(rec, new IntArrayList());
				recPosListMap.get(rec).add(pos);
			}
		}
		
		for ( Entry<Record, IntList> entry : recPosListMap.entrySet() ) {
			Record rec = entry.getKey();
			IntList idxList = entry.getValue();
			int sidx = idxList.get(0);
			IntSet denumSet = new IntOpenHashSet(query.getTokens());
			denumSet.add(sidx);
			for ( int j=sidx+1; j<idxList.size(); ++j ) {
				int eidx0 = idxList.get(j-1);
				int eidx1 = idxList.get(j);
				IntSet nextBlock = new IntOpenHashSet(rec.getTokenList().subList(eidx0+1, eidx1));
				if ( theta >= 1.0/nextBlock.size() ) {
				}
			}
		}
	}
	
	private void pruneRecordByPosition( Record query, Record rec, IntList idxList ) {
		IntSet denumSet = new IntOpenHashSet(query.getTokens());
		int i = 0;
		int sidx = idxList.get(i);
		int eidx0, eidx1 = sidx;
		denumSet.add(sidx);
		while ( i < idxList.size()-1 ) {
			eidx0 = eidx1;
			eidx1 = idxList.get(++i);
			IntSet nextBlock = new IntOpenHashSet(rec.getTokenList().subList(eidx0+1, eidx1));
			if ( theta >= 1.0/nextBlock.size() ) { // stop expansion
			}
			else { // continue expansion
			}
		}
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

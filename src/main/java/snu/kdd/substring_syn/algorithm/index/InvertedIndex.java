package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class InvertedIndex {
	
	final Int2ObjectMap<ObjectList<IndexEntry>> invList;
	final Int2ObjectMap<ObjectList<Record>> transInvList;
	int size;

	public InvertedIndex( Dataset dataset ) {
		invList = buildInvList(dataset);
		transInvList = buildTransIntList(dataset);
		size = computeSize();
	}

	private Int2ObjectMap<ObjectList<IndexEntry>> buildInvList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<IndexEntry>> invList = new Int2ObjectOpenHashMap<>();
		for ( Record rec : dataset.indexedList ) {
			for ( int i=0; i<rec.size(); ++i ) {
				int token = rec.getToken(i);
				if ( !invList.containsKey(token) ) invList.put(token, new ObjectArrayList<IndexEntry>());
				invList.get(token).add(new IndexEntry(rec, i));
			}
		}
		return invList;
	}
	
	private Int2ObjectMap<ObjectList<Record>> buildTransIntList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<Record>> transInvList = new Int2ObjectOpenHashMap<>();
		for ( Record rec : dataset.indexedList ) {
			for ( Rule rule : rec.getApplicableRuleIterable() ) {
				if ( rule.isSelfRule ) continue;
				for ( int token : rule.getRhs() ) {
					if ( !transInvList.containsKey(token) ) transInvList.put(token, new ObjectArrayList<Record>());
					transInvList.get(token).add(rec);
				}
			}
		}
		return transInvList;
	}
	
	private int computeSize() {
		int size = 0;
		for ( ObjectList<IndexEntry> list : invList.values() ) size += list.size();
		for ( ObjectList<Record> list : transInvList.values() ) size += list.size();
		return size;
	}
	
	public ObjectList<IndexEntry> getInvList( int token ) {
		return invList.get(token);
	}
	
	public ObjectList<Record> getTransInvList( int token ) {
		return transInvList.get(token);
	}
}

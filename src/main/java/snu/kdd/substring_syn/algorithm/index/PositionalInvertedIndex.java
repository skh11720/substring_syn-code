package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class PositionalInvertedIndex {
	
	final Int2ObjectMap<ObjectList<IndexEntry>> invList;
	final Int2ObjectMap<ObjectList<IndexEntry>> transInvList;
	int size;

	public PositionalInvertedIndex( Dataset dataset ) {
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
				invList.get(token).add( new IndexEntry(rec, i) );
			}
		}
		return invList;
	}
	
	private Int2ObjectMap<ObjectList<IndexEntry>> buildTransIntList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<IndexEntry>> transInvList = new Int2ObjectOpenHashMap<>();
		for ( Record rec : dataset.indexedList ) {
			for ( int k=0; k<rec.size(); ++k ) {
				for ( Rule rule : rec.getApplicableRules(k) ) {
					if ( rule.isSelfRule ) continue;
					for ( int token : rule.getRhs() ) {
						if ( !transInvList.containsKey(token) ) transInvList.put(token, new ObjectArrayList<IndexEntry>());
						transInvList.get(token).add(new IndexEntry(rec, k));
					}
				}
			}
		}
		return transInvList;
	}
	
	private int computeSize() {
		int size = 0;
		for ( ObjectList<IndexEntry> list : invList.values() ) size += list.size();
		for ( ObjectList<IndexEntry> list : transInvList.values() ) size += list.size();
		return size;
	}
	
	public ObjectList<IndexEntry> getInvList( int token ) {
		return invList.get(token);
	}
	
	public ObjectList<IndexEntry> getTransInvList( int token ) {
		return transInvList.get(token);
	}
	
	class IndexEntry {
		final Record rec;
		final int pos;
		
		public IndexEntry( Record rec, int pos ) {
			this.rec = rec;
			this.pos = pos;
		}
	}
}

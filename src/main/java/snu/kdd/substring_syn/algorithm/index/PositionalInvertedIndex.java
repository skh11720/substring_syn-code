package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;

public class PositionalInvertedIndex {
	
	final Int2ObjectMap<ObjectList<InvListEntry>> invList;
	final Int2ObjectMap<ObjectList<TransInvListEntry>> transInvList;
	int size;

	public PositionalInvertedIndex( Dataset dataset ) {
		invList = buildInvList(dataset);
		transInvList = buildTransIntList(dataset);
		size = computeSize();
	}

	private Int2ObjectMap<ObjectList<InvListEntry>> buildInvList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<InvListEntry>> invList = new Int2ObjectOpenHashMap<>();
		for ( Record rec : dataset.indexedList ) {
			for ( int i=0; i<rec.size(); ++i ) {
				int token = rec.getToken(i);
				if ( !invList.containsKey(token) ) invList.put(token, new ObjectArrayList<InvListEntry>());
				invList.get(token).add( new InvListEntry(rec, i) );
			}
		}
		return invList;
	}
	
	private Int2ObjectMap<ObjectList<TransInvListEntry>> buildTransIntList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<TransInvListEntry>> transInvList = new Int2ObjectOpenHashMap<>();
		for ( Record rec : dataset.indexedList ) {
			for ( int k=0; k<rec.size(); ++k ) {
				for ( Rule rule : rec.getApplicableRules(k) ) {
					if ( rule.isSelfRule ) continue;
					for ( int token : rule.getRhs() ) {
						if ( !transInvList.containsKey(token) ) transInvList.put(token, new ObjectArrayList<TransInvListEntry>());
						transInvList.get(token).add(new TransInvListEntry(rec, k, k+rule.lhsSize()-1));
					}
				}
			}
		}
		return transInvList;
	}
	
	private int computeSize() {
		int size = 0;
		for ( ObjectList<InvListEntry> list : invList.values() ) size += list.size();
		for ( ObjectList<TransInvListEntry> list : transInvList.values() ) size += list.size();
		return size;
	}
	
	public ObjectList<InvListEntry> getInvList( int token ) {
		return invList.get(token);
	}
	
	public ObjectList<TransInvListEntry> getTransInvList( int token ) {
		return transInvList.get(token);
	}
	
	class InvListEntry {
		final Record rec;
		final int pos;
		
		public InvListEntry( Record rec, int pos ) {
			this.rec = rec;
			this.pos = pos;
		}
	}
	
	class TransInvListEntry {
		final Record rec;
		final int left;
		final int right;
		
		public TransInvListEntry( Record rec, int left, int right ) {
			this.rec = rec;
			this.left = left;
			this.right = right;
		}
	}
}

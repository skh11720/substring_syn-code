package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class NaiveInvertedIndex {
	
	final Int2ObjectMap<ObjectList<TransformableRecordInterface>> invList;
	final Int2ObjectMap<ObjectList<TransformableRecordInterface>> transInvList;
	int size;

	public NaiveInvertedIndex( Dataset dataset ) {
		invList = buildInvList(dataset);
		transInvList = buildTransIntList(dataset);
		size = computeSize();
	}

	private Int2ObjectMap<ObjectList<TransformableRecordInterface>> buildInvList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<TransformableRecordInterface>> invList = new Int2ObjectOpenHashMap<>();
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			for ( int i=0; i<rec.size(); ++i ) {
				int token = rec.getToken(i);
				if ( !invList.containsKey(token) ) invList.put(token, new ObjectArrayList<TransformableRecordInterface>());
				invList.get(token).add(rec);
			}
		}
		return invList;
	}
	
	private Int2ObjectMap<ObjectList<TransformableRecordInterface>> buildTransIntList( Dataset dataset ) {
		Int2ObjectMap<ObjectList<TransformableRecordInterface>> transInvList = new Int2ObjectOpenHashMap<>();
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			rec.preprocessApplicableRules();
			for ( Rule rule : rec.getApplicableRuleIterable() ) {
				if ( rule.isSelfRule ) continue;
				for ( int token : rule.getRhs() ) {
					if ( !transInvList.containsKey(token) ) transInvList.put(token, new ObjectArrayList<TransformableRecordInterface>());
					transInvList.get(token).add(rec);
				}
			}
		}
		return transInvList;
	}
	
	private int computeSize() {
		int size = 0;
		for ( ObjectList<TransformableRecordInterface> list : invList.values() ) size += list.size();
		for ( ObjectList<TransformableRecordInterface> list : transInvList.values() ) size += list.size();
		return size;
	}
	
	public ObjectList<TransformableRecordInterface> getInvList( int token ) {
		return invList.get(token);
	}
	
	public ObjectList<TransformableRecordInterface> getTransInvList( int token ) {
		return transInvList.get(token);
	}
}

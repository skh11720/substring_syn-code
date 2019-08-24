package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedNaiveInvertedIndex {
	
	private final NaiveIndexStore store;
	private final InvertedListPool<Integer> invPool;
	private final InvertedListPool<Integer> tinvPool;
	
	private int nInvFault = 0;
	private int nTinvFault = 0;

	public DiskBasedNaiveInvertedIndex( Iterable<Record> recordList ) {
		store = new NaiveIndexStore(recordList);
		invPool = new InvertedListPool<>();
		tinvPool = new InvertedListPool<>();
	}

	public long invListSize() { return store.invListAccessor.size; }

	public long transInvListSize() { return store.tinvListAccessor.size; }
	
	public final int getNumInvFault() { return nInvFault; }

	public final int getNumTinvFault() { return nTinvFault; }

	public ObjectList<Integer> getInvList( int token ) {
		if ( invPool.containsKey(token) ) 
			return invPool.get(token);
		else {
			++nInvFault;
			ObjectList<Integer> invList = new ObjectArrayList<>(store.getInvList(token));
			invPool.put(token, invList);
			return invList;
		}
	}
	
	public ObjectList<Integer> getTransInvList( int token ) {
		if ( tinvPool.containsKey(token) )
			return tinvPool.get(token);
		else {
			++nTinvFault;
			ObjectList<Integer> tinvList = new ObjectArrayList<>(store.getTrInvList(token));
			tinvPool.put(token, tinvList);
			return tinvList;
		}
	}
}

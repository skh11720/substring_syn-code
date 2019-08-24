package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public abstract class AbstractDiskBasedInvertedIndex<S, T> {

	protected int nInvFault = 0;
	protected int nTinvFault = 0;
	protected final InvertedListPool<S> invPool;
	protected final InvertedListPool<T> tinvPool;
	
	public AbstractDiskBasedInvertedIndex( Iterable<Record> recordList ) {
		invPool = new InvertedListPool<>();
		tinvPool = new InvertedListPool<>();
	}
	
	public final int getNumInvFault() { return nInvFault; }

	public final int getNumTinvFault() { return nTinvFault; }
	
	protected abstract ObjectList<S> getInvListFromStore( int token );

	protected abstract ObjectList<T> getTinvListFromStore( int token );

	public ObjectList<S> getInvList( int token ) {
		if ( invPool.containsKey(token) ) 
			return invPool.get(token);
		else {
			++nInvFault;
			ObjectList<S> invList = getInvListFromStore(token);
			if ( invList != null ) invPool.put(token, invList);
			return invList;
		}
	}
	
	public ObjectList<T> getTransInvList( int token ) {
		if ( tinvPool.containsKey(token) )
			return tinvPool.get(token);
		else {
			++nTinvFault;
			ObjectList<T> tinvList = getTinvListFromStore(token);
			if ( tinvList != null ) tinvPool.put(token, tinvList);
			return tinvList;
		}
	}
}

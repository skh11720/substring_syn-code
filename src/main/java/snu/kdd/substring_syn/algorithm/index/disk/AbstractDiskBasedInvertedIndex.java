package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.BytesMeasurableInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractDiskBasedInvertedIndex<S extends BytesMeasurableInterface, T extends BytesMeasurableInterface> {

	protected int nInvFault = 0;
	protected int nTinvFault = 0;
	protected final InvertedListPool<S> invPool;
	protected final InvertedListPool<T> tinvPool;
	
	public AbstractDiskBasedInvertedIndex( Iterable<TransformableRecordInterface> recordList ) {
		invPool = new InvertedListPool<>();
		tinvPool = new InvertedListPool<>();
	}
	
	public final int getNumInvFault() { return nInvFault; }

	public final int getNumTinvFault() { return nTinvFault; }
	
	public abstract BigInteger diskSpaceUsage();
	
	protected abstract S copyInvList(S obj);

	protected abstract T copyTransInvList(T obj);
	
	protected abstract S getInvListFromStore( int token );

	protected abstract T getTinvListFromStore( int token );

	public S getInvList( int token ) {
		if ( invPool.containsKey(token) ) 
			return invPool.get(token);
		else {
			++nInvFault;
			StatContainer.global.increment("Num_InvFault");
			S invList = getInvListFromStore(token);
			if ( invList != null ) invPool.put(token, copyInvList(invList));
			return invList;
		}
	}
	
	public T getTransInvList( int token ) {
		if ( tinvPool.containsKey(token) )
			return tinvPool.get(token);
		else {
			++nTinvFault;
			StatContainer.global.increment("Num_TinvFault");
			T tinvList = getTinvListFromStore(token);
			if ( tinvList != null ) tinvPool.put(token, copyTransInvList(tinvList));
			return tinvList;
		}
	}
}

package snu.kdd.substring_syn.object.indexstore;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.IntPair;

public class InvertedIndex {

	protected int nPageFault = 0;
	protected final InvListAccessor invListAccessor;
	protected final InvertedListPool invPool;
	
	public InvertedIndex( Iterable<IntPair> kvList ) {
		IndexBuilder builder = new IndexBuilder(kvList);
		invListAccessor = builder.buildInvList();
		invPool = new InvertedListPool();
	}

	public long invListSize() { return invListAccessor.size; }
	
	public final int getNumInvFault() { return nPageFault; }

	public IntList getInvList( int key ) {
		if ( invPool.containsKey(key) ) 
			return invPool.get(key);
		else {
			++nPageFault;
			IntList invList = getInvListFromStore(key);
			if ( invList != null ) invPool.put(key, invList);
			return invList;
		}
	}
	
	protected IntList getInvListFromStore(int key) {
        IntList invList = invListAccessor.getList(key);
        return invList;
	}
}

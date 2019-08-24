package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedNaiveInvertedIndex extends AbstractDiskBasedInvertedIndex<Integer, Integer> {
	
	protected final NaiveIndexStore store;
	
	public DiskBasedNaiveInvertedIndex( Iterable<Record> recordList ) {
		super(recordList);
		store = new NaiveIndexStore(recordList);
	}

	public long invListSize() { return store.invListAccessor.size; }

	public long transInvListSize() { return store.tinvListAccessor.size; }

	@Override
	protected ObjectList<Integer> getInvListFromStore(int token) {
        IntList rawInvList = store.getInvList(token);
        if ( rawInvList == null ) return null;
        else return new ObjectArrayList<>(rawInvList);
	}

	@Override
	protected ObjectList<Integer> getTinvListFromStore(int token) {
        IntList rawTrInvList = store.getTrInvList(token);
        if ( rawTrInvList == null ) return null;
        else {
            ObjectList<Integer> tinvList = new ObjectArrayList<>(rawTrInvList);
            tinvPool.put(token, tinvList);
            return tinvList;
        }
	}
}

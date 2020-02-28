package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedNaiveInvertedIndex extends AbstractDiskBasedInvertedIndex<NaiveInvList, NaiveInvList> {
	
	protected final NaiveIndexStore store;
	
	public DiskBasedNaiveInvertedIndex( Iterable<Record> recordList ) {
		super(recordList);
		store = new NaiveIndexStore(recordList);
	}

	public long invListSize() { return store.invListAccessor.size; }

	public long transInvListSize() { return store.tinvListAccessor.size; }

	@Override
	protected NaiveInvList copyInvList(NaiveInvList obj) {
		return new NaiveInvList(obj);
	}

	@Override
	protected NaiveInvList copyTransInvList(NaiveInvList obj) {
		return new NaiveInvList(obj);
	}

	@Override
	protected NaiveInvList getInvListFromStore(int token) {
        return store.getInvList(token);
	}

	@Override
	protected NaiveInvList getTinvListFromStore(int token) {
        return store.getTrInvList(token);
	}

	@Override
	public BigInteger diskSpaceUsage() {
		return store.diskSpaceUsage();
	}
}

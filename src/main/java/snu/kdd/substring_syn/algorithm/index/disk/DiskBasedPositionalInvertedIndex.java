package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedPositionalInvertedIndex extends AbstractDiskBasedInvertedIndex<PositionInvList, PositionTrInvList> {

	protected final PositionalIndexStore store;

	public DiskBasedPositionalInvertedIndex(Iterable<Record> recordList) {
		super(recordList);
		store = new PositionalIndexStore(recordList);
	}

	public long invListSize() { return store.invListAccessor.size; }

	public long transInvListSize() { return store.tinvListAccessor.size; }
	
	@Override
	protected PositionInvList copyInvList(PositionInvList obj) {
		return new PositionInvList(obj);
	}
	
	@Override
	protected PositionTrInvList copyTransInvList(PositionTrInvList obj) {
		return new PositionTrInvList(obj);
	}

	@Override
	protected PositionInvList getInvListFromStore(int token) {
		return store.getInvList(token);
	}

	@Override
	protected PositionTrInvList getTinvListFromStore(int token) {
		return store.getTrInvList(token);
	}

	@Override
	public BigInteger diskSpaceUsage() {
		return store.diskSpaceUsage();
	}
}

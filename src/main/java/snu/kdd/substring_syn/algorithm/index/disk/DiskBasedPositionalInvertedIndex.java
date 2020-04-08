package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.InmemPositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.InmemPositionTrInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class DiskBasedPositionalInvertedIndex extends AbstractDiskBasedInvertedIndex<PositionInvList, PositionTrInvList> {

	protected final PositionalIndexStore store;

	public DiskBasedPositionalInvertedIndex(Iterable<TransformableRecordInterface> recordList) {
		super(recordList);
		store = new PositionalIndexStore(recordList);
	}

	public long invListSize() { return store.invListAccessor.size; }

	public long transInvListSize() { return store.tinvListAccessor.size; }
	
	@Override
	protected PositionInvList copyInvList(PositionInvList obj) {
		return InmemPositionInvList.copy(obj);
	}
	
	@Override
	protected PositionTrInvList copyTransInvList(PositionTrInvList obj) {
		return InmemPositionTrInvList.copy(obj);
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

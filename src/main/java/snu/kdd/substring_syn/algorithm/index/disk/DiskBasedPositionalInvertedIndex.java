package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalIndexInterface.InvListEntry;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedPositionalIndexInterface.TransInvListEntry;
import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedPositionalInvertedIndex extends AbstractDiskBasedInvertedIndex<InvListEntry, TransInvListEntry> {

	protected final PositionalIndexStore store;

	public DiskBasedPositionalInvertedIndex(Iterable<Record> recordList) {
		super(recordList);
		store = new PositionalIndexStore(recordList);
	}

	public long invListSize() { return store.invListAccessor.size; }

	public long transInvListSize() { return store.tinvListAccessor.size; }

	@Override
	protected ObjectList<InvListEntry> getInvListFromStore(int token) {
		return store.getInvList(token);
	}

	@Override
	protected ObjectList<TransInvListEntry> getTinvListFromStore(int token) {
		return store.getTrInvList(token);
	}
}

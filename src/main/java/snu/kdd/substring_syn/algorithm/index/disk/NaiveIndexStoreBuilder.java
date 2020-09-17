package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class NaiveIndexStoreBuilder extends AbstractIndexStoreBuilder {
	
	public final String storeName;

	public NaiveIndexStoreBuilder(Iterable<TransformableRecordInterface> recordList) {
		this(recordList, "NaiveIndexStore");
	}

	public NaiveIndexStoreBuilder(Iterable<TransformableRecordInterface> recordList, String storeName ) {
		super(recordList);
		this.storeName = storeName;
	}

	@Override
	public IndexStoreAccessor buildInvList() {
		return createIndexStoreAccessor(recordList, getInvPath(), this::buildInvListSegment);
	}

	@Override
	public IndexStoreAccessor buildTrInvList() {
		return createIndexStoreAccessor(recordList, getTinvPath(), this::buildTrInvListSegment);
	}

	@Override
	protected void addToInvList( IntArrayList list, TransformableRecordInterface rec, int pos ) {
		list.add(rec.getIdx());
	}

	@Override
	protected void addToTrInvList( IntArrayList list, TransformableRecordInterface rec, int pos, Rule rule ) {
		list.add(rec.getIdx());
	}

	@Override
	protected int invListEntrySize() {
		return 1;
	}

	@Override
	protected int trInvListEntrySize() {
		return 1;
	}

	@Override
	protected String getIndexStoreName() {
		return storeName;
	}
}

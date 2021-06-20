package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class PositionalIndexStoreBuilder extends AbstractIndexStoreBuilder {

	public PositionalIndexStoreBuilder(Iterable<TransformableRecordInterface> recordList) {
		super(recordList);
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
	protected void addToInvList(IntArrayList list, TransformableRecordInterface rec, int pos) {
		list.add(rec.getIdx());
		list.add(pos);
	}

	@Override
	protected void addToTrInvList(IntArrayList list, TransformableRecordInterface rec, int pos, Rule rule) {
		int n = list.size();
		if ( n >= 3 && list.getInt(n-3) == rec.getIdx() && list.getInt(n-2) == pos && list.getInt(n-1) == pos+rule.lhsSize()-1 ) return;
		list.add(rec.getIdx());
		list.add(pos);
		list.add(pos+rule.lhsSize()-1);
	}

	@Override
	protected int invListEntrySize() {
		return 2;
	}

	@Override
	protected int trInvListEntrySize() {
		return 3;
	}

	@Override
	protected String getIndexStoreName() {
		return "PositionalIndexStore";
	}
}

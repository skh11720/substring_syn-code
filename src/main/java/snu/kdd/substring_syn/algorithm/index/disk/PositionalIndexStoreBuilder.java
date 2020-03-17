package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;

public class PositionalIndexStoreBuilder extends AbstractIndexStoreBuilder {

	public PositionalIndexStoreBuilder(Iterable<Record> recordList) {
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
	protected void addToInvList(IntList list, Record rec, int pos) {
		list.add(rec.getIdx());
		list.add(pos);
	}

	@Override
	protected void addToTrInvList(IntList list, Record rec, int pos, Rule rule) {
		list.add(rec.getIdx());
		list.add(pos);
		list.add(pos+rule.lhsSize()-1);
	}

	@Override
	protected String getIndexStoreName() {
		return "PositionalIndexStore";
	}

}

package snu.kdd.substring_syn.algorithm.index.disk;

import snu.kdd.substring_syn.data.record.Record;

public class NaiveIndexStoreBuilder extends AbstractIndexStoreBuilder {

	public NaiveIndexStoreBuilder(Iterable<Record> recordList) {
		super(recordList);
	}

	public IndexStoreAccessor buildInvList() {
		return createIndexStoreAccessor(recordList, getInvPath(), this::buildInvListSegment);
	}

	public IndexStoreAccessor buildTrInvList() {
		return createIndexStoreAccessor(recordList, getTinvPath(), this::buildTrInvListSegment);
	}

	@Override
	protected String getIndexStoreName() {
		return "NaiveIndexStore";
	}
}

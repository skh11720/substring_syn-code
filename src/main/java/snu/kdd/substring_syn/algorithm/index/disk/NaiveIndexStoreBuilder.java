package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;

public class NaiveIndexStoreBuilder extends AbstractIndexStoreBuilder {

	public NaiveIndexStoreBuilder(Iterable<Record> recordList) {
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
	protected void addToInvList( IntList list, Record rec, int pos ) {
		list.add(rec.getID());
	}

	@Override
	protected void addToTrInvList( IntList list, Record rec, int pos, Rule rule ) {
		list.add(rec.getID());
	}

	@Override
	protected String getIndexStoreName() {
		return "NaiveIndexStore";
	}
}

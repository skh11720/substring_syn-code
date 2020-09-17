package snu.kdd.substring_syn.data;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public class DiskBasedDataset extends Dataset {
	
	protected RecordStore store;
	
	protected DiskBasedDataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset, RecordStore store) {
		super(statContainer, param, ruleset);
		this.store = store;
	}
	
	@Override
	public void addStat() {
		super.addStat();
		if (isDocInput()) statContainer.setStat(Stat.Dataset_numDoc, Long.toString(getNumDoc()));
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(store.getNumRecords()));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(store.getLenSum()));
		statContainer.setStat("Space_Recordstore", store.diskSpaceUsage().toString());
		statContainer.setStat("Num_QS_RecordFault", ""+store.getNumFaultQS());
		statContainer.setStat("Num_TS_RecordFault", ""+store.getNumFaultTS());
		statContainer.setStat("RecordPool_QS.capacity", ""+store.secQS.pool.capacity());
		statContainer.setStat("RecordPool_TS.capacity", ""+store.secTS.pool.capacity());
	}

	@Override
	public Iterable<TransformableRecordInterface> getIndexedList() {
		return store.getRecords();
	}

	@Override
	public Record getRawRecord(int idx) {
		return store.getRawRecord(idx);
	}

	@Override
	public TransformableRecordInterface getRecord(int idx) {
		return store.getRecord(idx);
	}
}

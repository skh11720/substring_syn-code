package snu.kdd.substring_syn.data;

import java.io.File;

import org.apache.commons.io.FileUtils;

import snu.kdd.substring_syn.data.record.Record;
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
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(store.getNumRecords()));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(store.getLenSum()));
		statContainer.setStat("Space_Recordstore", FileUtils.sizeOfAsBigInteger(new File(RecordStore.path)).toString());
		statContainer.setStat("Num_QS_RecordFault", ""+store.getNumFaultQS());
		statContainer.setStat("Num_TS_RecordFault", ""+store.getNumFaultTS());
	}

	@Override
	public Iterable<Record> getIndexedList() {
		return store.getRecords();
	}

	@Override
	public Record getRawRecord(int id) {
		return store.getRawRecord(id);
	}

	@Override
	public Record getRecord(int id) {
		return store.getRecord(id);
	}
}

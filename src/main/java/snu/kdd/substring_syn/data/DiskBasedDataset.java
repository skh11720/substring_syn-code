package snu.kdd.substring_syn.data;

import java.io.File;

import org.apache.commons.io.FileUtils;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class DiskBasedDataset extends Dataset {
	
	protected RecordStore store;
	
	protected DiskBasedDataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset, RecordStore store) {
		super(statContainer, param, ruleset);
		this.store = store;
	}
	
	@Override
	protected void addStat() {
		super.addStat();
		statContainer.setStat("Size_Recordstore", FileUtils.sizeOfAsBigInteger(new File(RecordStore.path)).toString());
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

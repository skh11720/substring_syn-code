package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class InMemDataset extends Dataset {
	
	private final List<Record> indexedList;
	
	protected InMemDataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset, Iterable<Record> indexedRecords) throws IOException {
		super(statContainer, param, ruleset);
		indexedList = new ObjectArrayList<>(indexedRecords.iterator());
	}

	@Override
	public Iterable<Record> getIndexedList() {
		return indexedList;
	}

	@Override
	public Record getRawRecord(int id) {
		return indexedList.get(id);
	}

	@Override
	public Record getRecord(int id) {
		Record rec = indexedList.get(id);
		rec.preprocessAll();
		return rec;
	}
}

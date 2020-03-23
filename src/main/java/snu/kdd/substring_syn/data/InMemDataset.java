package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public class InMemDataset extends Dataset {
	
	private final List<TransformableRecordInterface> indexedList;
	
	protected InMemDataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset, Iterable<TransformableRecordInterface> indexedRecords) throws IOException {
		super(statContainer, param, ruleset);
		indexedList = new ObjectArrayList<>(indexedRecords.iterator());
	}

	@Override
	public Iterable<TransformableRecordInterface> getIndexedList() {
		return indexedList;
	}

	@Override
	public Record getRawRecord(int idx) {
		return (Record)indexedList.get(idx);
	}

	@Override
	public TransformableRecordInterface getRecord(int idx) {
		Record rec = (Record)indexedList.get(idx);
		rec.preprocessAll();
		return rec;
	}
}

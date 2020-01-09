package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;

public class InMemDataset extends Dataset {
	
	private final List<Record> searchedList;
	private final List<Record> indexedList;
	
	protected InMemDataset(DatasetParam param) throws IOException {
		super(param);
		initTokenIndex();
		searchedList = new ObjectArrayList<>(new DiskBasedSearchedRecordIterator());
		indexedList = new ObjectArrayList<>(new DiskBasedIndexedRecordIterator());
	}
	
	@Override
	public Iterable<Record> getSearchedList() {
		return searchedList;
	}

	@Override
	public Iterable<Record> getIndexedList() {
		return indexedList;
	}

	@Override
	public Record getRecord(int id) {
		return indexedList.get(id);
	}
}

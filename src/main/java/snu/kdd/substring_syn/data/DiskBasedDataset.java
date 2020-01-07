package snu.kdd.substring_syn.data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedDataset extends AbstractDiskBasedDataset {
	
	final RecordStore store;
	
	protected DiskBasedDataset(DatasetParam param) throws IOException {
		super(param);
		initTokenIndex();
		store = new RecordStore(getIndexedList());
	}
	
	@Override
	public void addStat() {
		super.addStat();
		statContainer.setStat("Size_Recordstore", FileUtils.sizeOfAsBigInteger(new File(RecordStore.path)).toString());
	}
	
	@Override
	public Iterable<Record> getSearchedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new DiskBasedSearchedRecordIterator(searchedPath);
			}
		};
	}

	@Override
	public Iterable<Record> getIndexedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new DiskBasedIndexedRecordIterator(indexedPath);
			}
		};
	}

	@Override
	public Record getRecord(int id) {
		return store.getRecord(id);
	}
	
	public Iterable<Record> getRecords() {
		return store.getRecords();
	}
}

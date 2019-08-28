package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedDataset extends Dataset {
	
	final RecordStore store;
	
	protected DiskBasedDataset( String datasetName, String size, String nr, String qlen ) throws IOException {
		super(datasetName, size, nr, qlen);
		store = new RecordStore(getIndexedList());
	}
	
	@Override
	public Iterable<Record> getSearchedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new DiskBasedRecordIterator(searchedPath);
			}
		};
	}

	@Override
	public Iterable<Record> getIndexedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new DiskBasedRecordIterator(indexedPath);
			}
		};
	}

	@Override
	public Record getRecord(int id) {
		return store.getRecord(id);
	}
	
	class DiskBasedRecordIterator implements Iterator<Record> {
		
		BufferedReader br;
		Iterator<String> iter;
		int i = 0;
		
		public DiskBasedRecordIterator( String path ) {
			try {
				br = new BufferedReader(new FileReader(path));
				iter = br.lines().iterator();
			}
			catch ( IOException e ) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Record next() {
			String line = iter.next();
			return new Record(i++, line);
		}
		
	}
}

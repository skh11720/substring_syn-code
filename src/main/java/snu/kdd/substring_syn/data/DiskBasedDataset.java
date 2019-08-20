package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import snu.kdd.substring_syn.data.record.Record;

public class DiskBasedDataset extends Dataset {
	
	protected DiskBasedDataset( String name, String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		super(name, rulePath, searchedPath, indexedPath, outputPath);
	}
	
	public Iterable<Record> getSearchedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new DiskBasedRecordIterator(searchedPath);
			}
		};
	}

	public Iterable<Record> getIndexedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new DiskBasedRecordIterator(indexedPath);
			}
		};
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

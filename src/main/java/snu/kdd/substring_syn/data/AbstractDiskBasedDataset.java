package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import snu.kdd.substring_syn.data.record.Record;

public abstract class AbstractDiskBasedDataset extends Dataset {

	protected AbstractDiskBasedDataset(DatasetParam param) {
		super(param);
	}

	protected abstract class AbstractDiskBasedRecordIterator implements Iterator<Record> {
		
		BufferedReader br;
		Iterator<String> iter;
		int i = 0;
		
		public AbstractDiskBasedRecordIterator(String path) {
			try {
				br = new BufferedReader(new FileReader(path));
				iter = br.lines().iterator();
			}
			catch ( IOException e ) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	protected class DiskBasedSearchedRecordIterator extends AbstractDiskBasedRecordIterator {

		public DiskBasedSearchedRecordIterator(String path) {
			super(path);
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

	protected class DiskBasedIndexedRecordIterator extends AbstractDiskBasedRecordIterator {

		public DiskBasedIndexedRecordIterator(String path) {
			super(path);
		}

		@Override
		public boolean hasNext() {
			return i < size && iter.hasNext();
		}

		@Override
		public Record next() {
			String line = iter.next();
			String str = getPrefixWithLengthRatio(line);
			return new Record(i++, str);
		}
	}

}

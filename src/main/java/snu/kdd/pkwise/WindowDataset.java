package snu.kdd.pkwise;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class WindowDataset extends Dataset {

	private final List<Record> searchedList;

	public WindowDataset(String datasetName, String size, String nr, String qlen) {
		super(datasetName, size, nr, qlen);
		searchedList = loadRecordList(searchedPath);
	}

	@Override
	public Iterable<Record> getSearchedList() {
		return searchedList;
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
		return null;
	}

	private List<Record> loadRecordList( String dataPath ) {
		List<Record> recordList = new ObjectArrayList<>();
		try {
			BufferedReader br = new BufferedReader( new FileReader( dataPath ) );
			String line;
			for ( int i=0; ( line = br.readLine() ) != null; ++i ) {
				recordList.add( new Record( i, line ) );
			}
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		Log.log.info("loadRecordList(%s): %d records", dataPath, recordList.size());
		return recordList;
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

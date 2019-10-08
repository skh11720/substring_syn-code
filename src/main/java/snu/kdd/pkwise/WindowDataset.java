package snu.kdd.pkwise;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.RecordStore;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;

public class WindowDataset extends Dataset {

	private RecordStore store = null;
	private final List<Record> searchedList;

	public WindowDataset(String datasetName, String size, String nr, String qlen) {
		super(datasetName, size, nr, qlen);
		Record.tokenIndex = new TokenIndex();
		searchedList = loadRecordList(searchedPath);
	}
	
	public final void buildRecordStore() {
		store = new RecordStore(getIndexedList());
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
	
	public Iterable<Subrecord> getWindowList( int w ) {
		return new Iterable<Subrecord>() {
			
			@Override
			public Iterator<Subrecord> iterator() {
				return new WindowIterator(w);
			}
		};
	}
	
	@Override
	public Record getRecord(int id) {
		return store.getRecord(id);
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
	
	class WindowIterator implements Iterator<Subrecord> {

		Iterator<Record> rIter = new DiskBasedRecordIterator(indexedPath);
		Record rec = null;
		Record recNext = null;
		int widx = -1;
		final int w;
		
		public WindowIterator( int w ) {
			this.w = w;
			while ( rIter.hasNext() ) {
				rec = rIter.next();
				if ( rec.size() >= w ) break;
				else rec = null;
			}
			while ( rIter.hasNext() ) {
				recNext = rIter.next();
				if ( recNext.size() >= w ) break;
				else recNext = null;
			}
		}

		@Override
		public boolean hasNext() {
			return (recNext != null || widx +w < rec.size());
		}

		@Override
		public Subrecord next() {
			widx += 1;
			if ( widx+w > rec.size() ) {
				rec = recNext;
				recNext = null;
				while ( rIter.hasNext() ) {
					recNext = rIter.next();
					if ( recNext.size() >= w ) break;
					else recNext = null;
				}
				widx = 0;
			}
			return new Subrecord(rec, widx, widx+w);
		}
	}
}

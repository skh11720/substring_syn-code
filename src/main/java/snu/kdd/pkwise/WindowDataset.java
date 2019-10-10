package snu.kdd.pkwise;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.RecordStore;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;

public class WindowDataset extends Dataset {

	private RecordStore store = null;
	private List<Record> searchedList;

	public WindowDataset(String datasetName, String size, String nr, String qlen) {
		super(datasetName, size, nr, qlen);
		Record.tokenIndex = new TokenIndex();
		searchedList = loadRecordList(searchedPath);
	}
	
	public final void buildRecordStore() {
		searchedList = loadRecordList(searchedPath);
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
	
	public Iterable<Subrecord> getTransWindowList( int qlen, double theta ) {
		return new Iterable<Subrecord>() {
			
			@Override
			public Iterator<Subrecord> iterator() {
				return new TransWindowIterator(qlen, theta);
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

	class TransWindowIterator implements Iterator<Subrecord> {

		Iterator<Record> rIter = new DiskBasedRecordIterator(indexedPath);
		Record rec = null;
		TransLenCalculator transLen;
		int sidx, eidx;
		final int qlen;
		final double theta;
		
		public TransWindowIterator( int qlen, double theta ) {
			this.qlen = qlen;
			this.theta = theta;
			sidx = 0;
			eidx = -1;
			findNextWindow();
		}
		
		private void preprocessRecord() {
			rec.preprocessAll();
			transLen = new TransLenCalculator(null, rec, theta);
		}
		
		private void findNextWindow() {
			if ( rec != null && findNextWindowInRecord() ) return;
			rec = null;
			while ( rIter.hasNext() ) {
				rec = rIter.next();
				sidx = 0;
				eidx = -1;
				preprocessRecord();
				if ( findNextWindowInRecord() ) break;
				else rec = null;
			}
		}
		
		private boolean findNextWindowInRecord() {
			eidx += 1;
			for ( ; sidx<rec.size(); ++sidx ) {
				for ( ; eidx<rec.size(); ++eidx ) {
					if ( transLen.getLFLB(sidx, eidx) <= qlen && qlen <= transLen.getLFUB(sidx, eidx) ) {
					System.out.println( "len: "+(eidx-sidx+1) +"\t"+ "range: "+sidx+", "+eidx +"\t"+ "bound: "+transLen.getLFLB(sidx, eidx)+"\t"+transLen.getLFUB(sidx, eidx) );
//						System.out.println("TRUE");
						return true;
					}
				}
				eidx = sidx+1;
			}
			return false;
		}

		@Override
		public boolean hasNext() {
			return rec != null;
		}

		@Override
		public Subrecord next() {
			int sidx0 = sidx;
			int eidx0 = eidx;
			Record rec0 = rec;
			findNextWindow();
//			System.out.println(rec0.getID()+"\t"+sidx0+"\t"+eidx0+"\t"+(eidx0-sidx0+1));
			return new Subrecord(rec0, sidx0, eidx0);
		}
	}
}

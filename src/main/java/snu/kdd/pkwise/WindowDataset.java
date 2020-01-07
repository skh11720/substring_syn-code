package snu.kdd.pkwise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.data.AbstractDiskBasedDataset;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.RecordStore;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Util;

public class WindowDataset extends AbstractDiskBasedDataset {

	protected RecordStore recordStore = null;
	protected List<Record> searchedList;

	public WindowDataset(DatasetParam param) {
		super(param);
		Record.tokenIndex = new TokenIndex();
		searchedList = loadRecordList(searchedPath);
	}
	
	public final void buildRecordStore() {
		searchedList = loadRecordList(searchedPath);
		recordStore = new RecordStore(getIndexedList());
	}
	
	@Override
	public void addStat() {
		super.addStat();
		statContainer.setStat("Size_Recordstore", FileUtils.sizeOfAsBigInteger(new File(RecordStore.path)).toString());
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
				return new DiskBasedIndexedRecordIterator(indexedPath);
			}
		};
	}
	
	public Iterable<RecordInterface> getWindowList( int w ) {
		return new Iterable<RecordInterface>() {
			
			@Override
			public Iterator<RecordInterface> iterator() {
				return new WindowIterator(w);
			}
		};
	}

	public Iterable<RecordInterface> getWindowList( int wMin, int wMax ) {
		IterableConcatenator<RecordInterface> iconcat = new IterableConcatenator<>();
		for ( int w=wMin; w<=wMax; ++w ) iconcat.addIterable(getWindowList(w));
		return iconcat.iterable();
	}
	
	public Iterable<RecordInterface> getTransWindowList( int qlen, double theta ) {
		return new Iterable<RecordInterface>() {
			
			@Override
			public Iterator<RecordInterface> iterator() {
				return new TransWindowIterator(qlen, theta);
			}
		};
	}

	@Override
	public Record getRecord(int id) {
		return recordStore.getRecord(id);
	}

	public List<Record> loadRecordList( String dataPath ) {
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

	class WindowIterator implements Iterator<RecordInterface> {

		Iterator<Record> rIter = new DiskBasedIndexedRecordIterator(indexedPath);
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
			if ( rec.getID() == 7324 && w == 2 ) Log.log.trace("window: "+(new Subrecord(rec, widx, widx+w))+"\t"+(new Subrecord(rec, widx, widx+w)).toOriginalString());
			return new Subrecord(rec, widx, widx+w);
		}
	}

	class TransWindowIterator implements Iterator<RecordInterface> {

		Iterator<Record> rIter = new DiskBasedIndexedRecordIterator(indexedPath);
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
		
		protected void preprocessRecord() {
			rec.preprocessAll();
			double modifiedTheta = Util.getModifiedTheta(qlen, rec, theta);
			transLen = new TransLenCalculator(null, rec, modifiedTheta);
		}
		
		protected void findNextWindow() {
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
		
		protected boolean findNextWindowInRecord() {
			eidx += 1;
			for ( ; sidx<rec.size(); ++sidx ) {
				for ( ; eidx<rec.size(); ++eidx ) {
//					Log.log.trace("id: "+rec.getID()+"\t"+ "len: "+(eidx-sidx+1) +"\t"+ "range: "+sidx+", "+(eidx+1) +"\t"+ "bound: "+transLen.getLFLB(sidx, eidx)+"\t"+transLen.getLFUB(sidx, eidx) );
					if ( transLen.getLFLB(sidx, eidx) <= qlen && qlen <= transLen.getLFUB(sidx, eidx) ) {
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
			return new Subrecord(rec0, sidx0, eidx0+1);
		}
	}
}

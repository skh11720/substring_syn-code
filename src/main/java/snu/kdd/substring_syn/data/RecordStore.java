package snu.kdd.substring_syn.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import snu.kdd.substring_syn.data.record.Record;

public class RecordStore {

	public static final String path = "./tmp/RecordStore";
	private final Ruleset ruleset;
	private final LongList posList;
	private final byte[] buffer;
	private RandomAccessFile raf;
	
	
	public RecordStore(Dataset dataset) {
//		Log.log.trace("RecordStore.constructor");
		ruleset = dataset.ruleSet;
		posList = new LongArrayList();
		try {
			materializeRecords(dataset.getIndexedList());
			raf = new RandomAccessFile(path, "r");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		buffer = setBuffer();
	}
	
	private void materializeRecords( Iterable<Record> recordList ) throws IOException {
		long cur = 0;
		FileOutputStream fos = new FileOutputStream(path);
		for ( Record rec : recordList ) {
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			rec.getMaxRhsSize();
			posList.add(cur);
			byte[] b = rec.serialize();
			cur += b.length;
			fos.write(b);
//			Log.log.trace("RecordStore.materializeRecords: rec.id=%d, len=%d, cur=%d", rec.getID(), b.length, cur);
		}
		fos.close();
		posList.add(cur);
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posList.size()-1; ++i ) bufSize = Math.max(bufSize, (int)(posList.get(i+1)-posList.get(i)));
		return new byte[bufSize];
	}
	
	public Record getRecord( int id ) {
		try {
			return tryGetRecord(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public Record tryGetRecord( int id ) throws IOException {
		int len = (int)(posList.get(id+1) - posList.get(id));
		raf.seek(posList.get(id));
		raf.read(buffer, 0, len);
		return Record.deserialize(buffer, len, ruleset);
	}
	
	public Iterable<Record> getRecords() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new RecordIterator();
			}
		};
	}
	
	class RecordIterator implements Iterator<Record> {
		
		int i = 0;
		FileInputStream fis;
		
		public RecordIterator() {
			try {
				fis = new FileInputStream(path);
			}
			catch ( IOException e ) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		@Override
		public boolean hasNext() {
			return (i < posList.size()-1);
		}

		@Override
		public Record next() {
			Record rec = getRecord(i);
			i += 1;
			return rec;
		}
	}
}

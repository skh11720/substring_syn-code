package snu.kdd.substring_syn.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import snu.kdd.substring_syn.data.record.Record;

public class RecordStore {

	public static final String path = "./tmp/RecordStore";
	private final LongList posList;
	private final byte[] buffer;
	private RandomAccessFile raf;
	
	
	public RecordStore( Iterable<Record> recordList ) {
//		Log.log.trace("RecordStore.constructor");
		posList = new LongArrayList();
		try {
			materializeRecords(recordList);
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
//			Log.log.trace("RecordStore.materializeRecords: rec.id=%d, cur=%d", rec.getID(), cur);
			posList.add(cur);
			byte[] b = Snappy.compress(rec.getTokenArray());
			cur += b.length;
			fos.write(b);
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
		int[] tokens = Snappy.uncompressIntArray(buffer, 0, len);
		return new Record(id, tokens);
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
			int len = (int)(posList.get(i+1) - posList.get(i));
			int[] tokens = null;
			try {
				fis.read(buffer, 0, len);
				tokens = Snappy.uncompressIntArray(buffer, 0, len);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			Record rec = new Record(i, tokens);
			i += 1;
			return rec;
		}
	}
}

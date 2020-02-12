package snu.kdd.substring_syn.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordSerializer;
import snu.kdd.substring_syn.utils.FileBasedLongList;
import snu.kdd.substring_syn.utils.Log;

public class RecordStore {

	private final int MAX_BUF_SIZE = -1;
	private final Ruleset ruleset;
	private final byte[] buffer;
	
	private final RecordStoreSection secQS;
	private final RecordStoreSection secTS;
	
	private int numRecords = 0;
	private long lenSum = 0;
	
	
	class RecordStoreSection {
		final String path;
		final RecordPool pool;
		final FileBasedLongList posList;
		RandomAccessFile raf;
		int nRecFault = 0;
		
		public RecordStoreSection(String suffix, int capacity) {
			path = "./tmp/RecordStoreSection_"+suffix;
			pool = new RecordPool(capacity);
			posList = new FileBasedLongList("posList"+suffix);
		}
		
		public void init() {
			try {
				raf = new RandomAccessFile(path, "r");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	
	
	public RecordStore(Iterable<Record> indexedRecords, Ruleset ruleset) {
//		Log.log.trace("RecordStore.constructor");
		this.ruleset = ruleset;
		secQS = new RecordStoreSection("QS", RecordPool.BUFFER_SIZE);
		secTS = new RecordStoreSection("TS", 10*RecordPool.BUFFER_SIZE);
		try {
			materializeRecords(indexedRecords);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		buffer = setBuffer();
		secQS.init();
		secTS.init();
	}
	
	private void materializeRecords(Iterable<Record> recordList) throws IOException {
		Log.log.trace("recordStore.materializeRecords()");
		long curQS = 0;
		long curTS = 0;
		BufferedOutputStream bosQS = new BufferedOutputStream(new FileOutputStream(secQS.path));
		BufferedOutputStream bosTS = new BufferedOutputStream(new FileOutputStream(secTS.path));
		for ( Record rec : recordList ) {
			numRecords += 1;
			lenSum += rec.size();
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			rec.getMaxRhsSize();

			secQS.posList.add(curQS);
			RecordSerializer.shallowSerialize(rec);
			curQS += RecordSerializer.blen;
			bosQS.write(RecordSerializer.bbuf, 0, RecordSerializer.blen);

			secTS.posList.add(curTS);
			RecordSerializer.serialize(rec);
			curTS += RecordSerializer.blen;
			bosTS.write(RecordSerializer.bbuf, 0, RecordSerializer.blen);
//			Log.log.trace("RecordStore.materializeRecords: rec.id=%d, len=%d, cur=%d", rec.getID(), b.length, cur);
		}
		bosQS.close();
		secQS.posList.add(curQS);
		secQS.posList.finalize();
		bosTS.close();
		secTS.posList.add(curTS);
		secTS.posList.finalize();
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<secTS.posList.size()-1; ++i ) bufSize = Math.max(bufSize, (int)(secTS.posList.get(i+1)-secTS.posList.get(i)));
		bufSize = Math.max(bufSize, MAX_BUF_SIZE);
		return new byte[bufSize];
	}
	
	public Record getRecord( int id ) {
		try {
//			return tryGetRecord(id);
			return getRecordFromStore(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	@Deprecated
	@SuppressWarnings("unused")
	private Record tryGetRecord( int id ) throws IOException {
		if ( !secTS.pool.containsKey(id) ) {
			secTS.nRecFault += 1;
			loadRecordsFromStore(id);
		}
		return secTS.pool.get(id);
	}
	
	private void loadRecordsFromStore(int id) throws IOException {
		secTS.raf.seek(secTS.posList.get(id));
		secTS.raf.read(buffer, 0, buffer.length);
		for ( int i=id, lenRead=0; i<numRecords; ++i ) {
			int len = (int)( -secTS.posList.get(i) + secTS.posList.get(i+1) );
			if ( lenRead+len > buffer.length ) break;
			Record rec = RecordSerializer.deserialize(buffer, lenRead, len, ruleset);
			secTS.pool.put(i, rec);
			lenRead += len;
		}
	}
	
	private Record getRecordFromStore(int id) throws IOException {
		secTS.raf.seek(secTS.posList.get(id));
		secTS.raf.read(buffer, 0, buffer.length);
		int len = (int)( -secTS.posList.get(id) + secTS.posList.get(id+1) );
		return RecordSerializer.deserialize(buffer, 0, len, ruleset);
	}
	
	public Record getRawRecord( int id ) {
		try {
//			return tryGetRawRecord(id);
			return getRawRecordFromStore(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	@Deprecated
	@SuppressWarnings("unused")
	private Record tryGetRawRecord(int id) throws IOException {
		if ( !secQS.pool.containsKey(id) ) {
			secQS.nRecFault += 1;
			loadRawRecordsFromStore(id);
		}
		return secQS.pool.get(id);
	}

	private void loadRawRecordsFromStore( int id ) throws IOException {
		secQS.raf.seek(secQS.posList.get(id));
		secQS.raf.read(buffer, 0, buffer.length);
		for ( int i=id, lenRead=0; i<numRecords; ++i ) {
			int len = (int)( -secQS.posList.get(i) + secQS.posList.get(i+1) );
			if ( lenRead+len > buffer.length ) break;
			IntArrayList list = IntArrayList.wrap(Snappy.uncompressIntArray(buffer, lenRead, len));
			secQS.pool.put(i, new Record(list.getInt(0), list.subList(1, list.size()).toIntArray()));
			lenRead += len;
		}
	}
	
	private Record getRawRecordFromStore(int id) throws IOException {
		secQS.raf.seek(secQS.posList.get(id));
		secQS.raf.read(buffer, 0, buffer.length);
		int len = (int)( -secQS.posList.get(id) + secQS.posList.get(id+1) );
		IntArrayList list = IntArrayList.wrap(Snappy.uncompressIntArray(buffer, 0, len));
		return new Record(list.getInt(0), list.subList(1, list.size()).toIntArray());
	}
	
	public Iterable<Record> getRecords() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new RecordIterator();
			}
		};
	}
	
	public final int getNumFaultQS() { return secQS.nRecFault; }

	public final int getNumFaultTS() { return secTS.nRecFault; }
	
	public final int getNumRecords() { return numRecords; }

	public final long getLenSum() { return lenSum; }
	
//	class RecordIterator implements Iterator<Record> {
//		
//		int i = 0;
//		FileInputStream fis;
//		
//		public RecordIterator() {
//			try {
//				fis = new FileInputStream(secTS.path);
//			}
//			catch ( IOException e ) {
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}
//
//		@Override
//		public boolean hasNext() {
//			return (i < secTS.posList.size()-1);
//		}
//
//		@Override
//		public Record next() {
//			Record rec = getRecord(i);
//			i += 1;
//			return rec;
//		}
//	}
	
	public final BigInteger diskSpaceUsage() {
		return FileUtils.sizeOfAsBigInteger(new File(secQS.path))
				.add(FileUtils.sizeOfAsBigInteger(new File(secTS.path)));
	}

	class RecordIterator implements Iterator<Record> {
		
		int id = 0;
		int offset = 0;
		int len;
		FileInputStream fis;
		byte[] b = new byte[RecordSerializer.bbuf.length*100];
		
		public RecordIterator() {
			try {
				fis = new FileInputStream(secTS.path);
				fis.read(b, 0, b.length);
			}
			catch ( IOException e ) {
				e.printStackTrace();
				System.exit(1);
			}
			len = (int)(-secTS.posList.get(id) + secTS.posList.get(id+1));
		}

		@Override
		public boolean hasNext() {
			return (id < secTS.posList.size()-1);
		}

		@Override
		public Record next() {
			Record rec = RecordSerializer.deserialize(b, offset, len, ruleset);
			findNext();
			return rec;
		}
		
		private void findNext() {
			id += 1;
			offset += len;
			if ( !hasNext() ) return;
			len = (int)(secTS.posList.get(id+1) - secTS.posList.get(id));
			if ( offset+len > b.length ) {
				for ( int i=offset; i<b.length; ++i ) b[i-offset] = b[i];
				try {
					fis.read(b, b.length-offset, offset);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				offset = 0;
			}
		}
	}
}
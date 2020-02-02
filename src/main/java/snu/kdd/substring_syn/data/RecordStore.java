package snu.kdd.substring_syn.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordSerializer;
import snu.kdd.substring_syn.utils.FileBasedLongList;
import snu.kdd.substring_syn.utils.Log;

public class RecordStore {

	public static final String path = "./tmp/RecordStore";
	private final Ruleset ruleset;
	private final FileBasedLongList posListQS;
	private final FileBasedLongList posListTS;
	private final byte[] buffer;
	private RandomAccessFile raf;
	private final RecordPool poolQS;
	private final RecordPool poolTS;
	
	private int nRecFaultQS = 0;
	private int nRecFaultTS = 0;
	
	private int numRecords = 0;
	private long lenSum = 0;
	
	
	public RecordStore(Iterable<Record> indexedRecords, Ruleset ruleset) {
//		Log.log.trace("RecordStore.constructor");
		this.ruleset = ruleset;
		posListQS = new FileBasedLongList("posListQS");
		posListTS = new FileBasedLongList("posListTS");
		try {
			materializeRecords(indexedRecords);
			raf = new RandomAccessFile(path, "r");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		buffer = setBuffer();
		poolQS = new RecordPool();
		poolTS = new RecordPool();
	}
	
	private void materializeRecords(Iterable<Record> recordList) throws IOException {
		Log.log.trace("recordStore.materializeRecords()");
		long cur = 0;
		FileOutputStream fos = new FileOutputStream(path);
		for ( Record rec : recordList ) {
			numRecords += 1;
			lenSum += rec.size();
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			rec.getMaxRhsSize();
			posListQS.add(cur);
			RecordSerializer.shallowSerialize(rec);
			cur += RecordSerializer.blen;
			fos.write(RecordSerializer.bbuf, 0, RecordSerializer.blen);
			posListTS.add(cur);
			RecordSerializer.serialize(rec);
			cur += RecordSerializer.blen;
			fos.write(RecordSerializer.bbuf, 0, RecordSerializer.blen);
//			Log.log.trace("RecordStore.materializeRecords: rec.id=%d, len=%d, cur=%d", rec.getID(), b.length, cur);
		}
		fos.close();
		posListQS.add(cur);
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posListTS.size()-1; ++i ) bufSize = Math.max(bufSize, (int)(posListQS.get(i+1)-posListTS.get(i)));
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
	
	private Record tryGetRecord( int id ) throws IOException {
		if ( poolTS.containsKey(id) ) return poolTS.get(id);
		else {
			Record rec = getRecordFromStore(id);
			poolTS.put(id, rec);
			nRecFaultTS += 1;
			return rec;
		}
	}
	
	private Record getRecordFromStore(int id) throws IOException {
		int len = (int)(posListQS.get(id+1) - posListTS.get(id));
		raf.seek(posListTS.get(id));
		raf.read(buffer, 0, len);
		return RecordSerializer.deserialize(buffer, len, ruleset);
	}
	
	public Record getRawRecord( int id ) {
		try {
			return tryGetRawRecord(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	private Record tryGetRawRecord(int id) throws IOException {
		if ( poolQS.containsKey(id) ) return poolQS.get(id);
		else {
			Record rec = getRawRecordFromStore(id);
			poolQS.put(id, rec);
			nRecFaultQS += 1;
			return rec;
		}
	}

	private Record getRawRecordFromStore( int id ) throws IOException {
		int len = (int)(posListTS.get(id) - posListQS.get(id));
		raf.seek(posListQS.get(id));
		raf.read(buffer, 0, len);
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
	
	public final int getNumFaultQS() { return nRecFaultQS; }

	public final int getNumFaultTS() { return nRecFaultTS; }
	
	public final int getNumRecords() { return numRecords; }

	public final long getLenSum() { return lenSum; }
	
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
			return (i < posListQS.size()-1);
		}

		@Override
		public Record next() {
			Record rec = getRecord(i);
			i += 1;
			return rec;
		}
	}
}

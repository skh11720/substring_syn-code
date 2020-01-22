package snu.kdd.substring_syn.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import snu.kdd.substring_syn.data.record.Record;

public class RecordStore {

	public static final String path = "./tmp/RecordStore";
	private final Ruleset ruleset;
	private final LongList posListQS;
	private final LongList posListTS;
	private final byte[] buffer;
	private RandomAccessFile raf;
	
	
	public RecordStore(Iterable<Record> indexedRecords, Ruleset ruleset) {
//		Log.log.trace("RecordStore.constructor");
		this.ruleset = ruleset;
		posListQS = new LongArrayList();
		posListTS = new LongArrayList();
		try {
			materializeRecords(indexedRecords);
			raf = new RandomAccessFile(path, "r");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		buffer = setBuffer();
	}
	
	private void materializeRecords(Iterable<Record> recordList) throws IOException {
		long cur = 0;
		byte[] b;
		FileOutputStream fos = new FileOutputStream(path);
		for ( Record rec : recordList ) {
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			rec.getMaxRhsSize();
			posListQS.add(cur);
			IntArrayList idAndTokens = new IntArrayList();
			idAndTokens.add(rec.getID());
			idAndTokens.addAll(rec.getTokens());
			b = Snappy.compress(idAndTokens.toIntArray());
			cur += b.length;
			fos.write(b);
			posListTS.add(cur);
			b = rec.serialize();
			cur += b.length;
			fos.write(b);
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
		int len = (int)(posListQS.get(id+1) - posListTS.get(id));
		raf.seek(posListTS.get(id));
		raf.read(buffer, 0, len);
		return Record.deserialize(buffer, len, ruleset);
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

	private Record tryGetRawRecord( int id ) throws IOException {
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

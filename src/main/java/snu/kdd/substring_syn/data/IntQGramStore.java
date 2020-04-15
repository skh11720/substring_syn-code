package snu.kdd.substring_syn.data;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.FileBasedLongList;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Util;

public class IntQGramStore {

	public static final String path = "./tmp/IntQGramStore";
	protected static final long FILE_MAX_LEN = 8_000_000_000_000_000_000L;
	private final FileBasedLongList posList;
	private final byte[] buffer;
	private RandomAccessFile raf;
	
	
	public IntQGramStore( Iterable<IntQGram> iqgramList ) {
		posList = new FileBasedLongList("IntQGramStore.posList");
		try {
			materializeIntQGrams(iqgramList);
			raf = new RandomAccessFile(path, "r");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		buffer = setBuffer();
	}
	
	private void materializeIntQGrams( Iterable<IntQGram> iqgramList ) throws IOException {
		long cur = 0;
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
		for ( IntQGram iqgram : iqgramList ) {
			posList.add(cur);
			byte[] b = Snappy.compress(iqgram.arr);
			cur += b.length;
			bos.write(b);
			if ( (posList.size() % 1_000_000) == 0 ) Log.log.info("materializeIntQGrams: posList.size="+
					NumberFormat.getNumberInstance().format(posList.size())+"\tcur="+
					NumberFormat.getNumberInstance().format(cur));
		}
		bos.close();
		posList.add(cur);
		posList.finalize();
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posList.size()-1; ++i ) bufSize = Math.max(bufSize, (int)(posList.get(i+1)-(posList.get(i))));
		return new byte[bufSize];
	}
	
	public IntQGram getIntQGram( int idx ) {
		try {
			return tryGetIntQGram(idx);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public IntQGram tryGetIntQGram( int idx ) throws IOException {
		int len = (int)(posList.get(idx+1)-(posList.get(idx)));
		raf.seek(posList.get(idx));
		raf.read(buffer, 0, len);
		int[] arr = Snappy.uncompressIntArray(buffer, 0, len);
		return new IntQGram(arr);
	}
	
	public Iterable<TransformableRecordInterface> getIntQGrams() {
		return new Iterable<TransformableRecordInterface>() {
			
			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				return new IntQGramIterator();
			}
		};
	}
	
	public final int getNumIntQGrams() {
		return posList.size();
	}
	
	public final BigInteger diskSpaceUsage() {
		return Util.getSpaceUsage(path);
	}

	class IntQGramIterator implements Iterator<TransformableRecordInterface> {
		
		int i = 0;
		FileInputStream fis;
		
		public IntQGramIterator() {
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
		public TransformableRecordInterface next() {
			int len = (int)(posList.get(i+1)-(posList.get(i)));
			int[] arr = null;
			try {
				fis.read(buffer, 0, len);
				arr = Snappy.uncompressIntArray(buffer, 0, len);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			Record iqgram = new Record(arr);
			i += 1;
			return iqgram;
		}
	}
}

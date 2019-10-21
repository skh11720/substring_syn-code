package snu.kdd.substring_syn.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class IntQGramStore {

	public static final String path = "./tmp/IntQGramStore";
	protected static final long FILE_MAX_LEN = 8_000_000_000_000_000_000L;
	private final ObjectList<BigInteger> posList;
	private final byte[] buffer;
	private RandomAccessFile raf;
	
	
	public IntQGramStore( Iterable<IntQGram> iqgramList ) {
		posList = new ObjectArrayList<>();
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
		BigInteger cur = BigInteger.ZERO;
		FileOutputStream fos = new FileOutputStream(path);
		for ( IntQGram iqgram : iqgramList ) {
			posList.add(cur);
			byte[] b = Snappy.compress(iqgram.arr);
			cur = cur.add(BigInteger.valueOf(b.length));
			fos.write(b);
			if ( (posList.size() % 1_000_000) == 0 ) Log.log.info("materializeIntQGrams: posList.size="+
					NumberFormat.getNumberInstance().format(posList.size())+"\tcur="+
					NumberFormat.getNumberInstance().format(cur));
		}
		fos.close();
		posList.add(cur);
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posList.size()-1; ++i ) bufSize = Math.max(bufSize, posList.get(i+1).subtract(posList.get(i)).intValueExact());
		return new byte[bufSize];
	}
	
	public IntQGram getIntQGram( int id ) {
		try {
			return tryGetIntQGram(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public IntQGram tryGetIntQGram( int id ) throws IOException {
		int len = posList.get(id+1).subtract(posList.get(id)).intValueExact();
		raf.seek(posList.get(id).longValueExact());
		raf.read(buffer, 0, len);
		int[] arr = Snappy.uncompressIntArray(buffer, 0, len);
		return new IntQGram(arr);
	}
	
	public Iterable<Record> getIntQGrams() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new IntQGramIterator();
			}
		};
	}
	
	public final int getNumIntQGrams() {
		return posList.size();
	}
	
	class IntQGramIterator implements Iterator<Record> {
		
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
		public Record next() {
			int len = posList.get(i+1).subtract(posList.get(i)).intValueExact();
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

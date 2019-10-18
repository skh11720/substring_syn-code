package snu.kdd.substring_syn.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class IntQGramStore {

	private static final String path = "./tmp/IntQGramStore";
	private final IntList posList;
	private final byte[] buffer;
	private RandomAccessFile raf;
	
	
	public IntQGramStore( Iterable<IntQGram> iqgramList ) {
		posList = new IntArrayList();
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
		int cur = 0;
		FileOutputStream fos = new FileOutputStream(path);
		for ( IntQGram iqgram : iqgramList ) {
			posList.add(cur);
			byte[] b = Snappy.compress(iqgram.arr);
			cur += b.length;
			fos.write(b);
		}
		fos.close();
		posList.add(cur);
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posList.size()-1; ++i ) bufSize = Math.max(bufSize, posList.get(i+1)-posList.get(i));
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
		int len = posList.get(id+1) - posList.get(id);
		raf.seek(posList.get(id));
		raf.read(buffer, 0, len);
		int[] arr = Snappy.uncompressIntArray(buffer, 0, len);
		return new IntQGram(arr);
	}
	
	public Iterable<IntQGram> getIntQGrams() {
		return new Iterable<IntQGram>() {
			
			@Override
			public Iterator<IntQGram> iterator() {
				return new IntQGramIterator();
			}
		};
	}
	
	class IntQGramIterator implements Iterator<IntQGram> {
		
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
		public IntQGram next() {
			int len = posList.get(i+1) - posList.get(i);
			int[] arr = null;
			try {
				fis.read(buffer, 0, len);
				arr = Snappy.uncompressIntArray(buffer, 0, len);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			IntQGram iqgram = new IntQGram(arr);
			i += 1;
			return iqgram;
		}
	}
}

package snu.kdd.substring_syn.object.indexstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class EntryStore<E extends Serializable> {

	private final IntList posList;
	private final byte[] buffer;
	private RandomAccessFile raf;
	private int size = 0;
	private final String path;
	
	
	public EntryStore( Iterable<E> entryList, String name ) {
		posList = new IntArrayList();
		path = String.format("./tmp/%s", name);
		try {
			materializeEntries(entryList);
			raf = new RandomAccessFile(path, "r");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		buffer = setBuffer();
	}
	
	protected byte[] serialize(E entry) {
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(entry);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return bos.toByteArray();
	}
	
	@SuppressWarnings("unchecked")
	protected E deserialize(byte[] buf, int offset, int length) {
		ByteArrayInputStream bis = new ByteArrayInputStream(buf, offset, length);
		E entry = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(bis);
			entry = (E) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return entry;
	}
	
	private void materializeEntries( Iterable<E> entryList ) throws IOException {
		int cur = 0;
		FileOutputStream fos = new FileOutputStream(path);
		for ( E entry : entryList ) {
			posList.add(cur);
			byte[] b = serialize(entry);
//			byte[] b = Snappy.compress(rec.getTokenArray());
			cur += b.length;
			fos.write(b);
			size += 1;
		}
		fos.close();
		posList.add(cur);
	}
	
	private byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posList.size()-1; ++i ) bufSize = Math.max(bufSize, posList.get(i+1)-posList.get(i));
		return new byte[bufSize];
	}
	
	public E getEntry( int id ) {
		try {
			return tryGetEntry(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public E tryGetEntry( int id ) throws IOException {
		int len = posList.get(id+1) - posList.get(id);
		raf.seek(posList.get(id));
		raf.read(buffer, 0, len);
//		int[] tokens = Snappy.uncompressIntArray(buffer, 0, len);
		E entry = deserialize(buffer, 0, len);
		return entry;
	}
	
	public Iterable<E> getEntries() {
		return new Iterable<E>() {
			
			@Override
			public Iterator<E> iterator() {
				return new EntryIterator();
			}
		};
	}
	
	public final int size() { return size; }
	
	public final BigInteger diskSpaceUsage() {
		return FileUtils.sizeOfAsBigInteger(new File(path));
	}
	
	class EntryIterator implements Iterator<E> {
		
		int i = 0;
		FileInputStream fis;
		
		public EntryIterator() {
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
		public E next() {
			int len = posList.get(i+1) - posList.get(i);
			E entry = null;
			try {
				fis.read(buffer, 0, len);
				entry = deserialize(buffer, 0, len);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			i += 1;
			return entry;
		}
	}
}

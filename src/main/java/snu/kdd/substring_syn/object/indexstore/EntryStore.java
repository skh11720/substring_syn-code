package snu.kdd.substring_syn.object.indexstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.utils.Log;

public class EntryStore<E extends Serializable> {

	protected final long MAX_FILE_SIZE = 10L * 1024 * 1024 * 1024;
	protected final long MAX_STORE_SIZE = 10L * 1024 * 1024 * 1024;
	private final IntList maxIdList;
	private final LongList posList;
	private final ObjectList<RandomAccessFile> rafList;
	private final byte[] buffer;
	private final String path;

	public final int numEntries;
	public final long storeSize;
	
	
	
	public EntryStore(Iterable<E> entryList, String name) {
		maxIdList = new IntArrayList();
		posList = new LongArrayList();
		rafList = new ObjectArrayList<RandomAccessFile>();
		path = String.format("./tmp/%s", name);
		Cursor cur = materializeEntries(entryList);
		numEntries = cur.numEntries;
		storeSize = cur.storeSize;
		buffer = setBuffer();
	}
	
	protected final byte[] serialize(E entry) {
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
	protected final E deserialize(byte[] buf, int offset, int length) {
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
	
	private final Cursor materializeEntries(Iterable<E> entryList) {
		Cursor cur = null;
		posList.add(0);
		try {
			cur = new Cursor();
			for ( E entry : entryList ) {
				byte[] b = serialize(entry);
//			byte[] b = Snappy.compress(rec.getTokenArray());
				cur.write(b);
				posList.add(cur.offset);
				cur.updateCursorIfNecessary();
				cur.checkStoreSizeOverflow();
			}
			cur.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return cur;
	}
	
	private final byte[] setBuffer() {
		int bufSize = 0;
		for ( int i=0; i<posList.size()-1; ++i ) bufSize = Math.max(bufSize, (int)(posList.getLong(i+1)-posList.getLong(i)));
		return new byte[bufSize];
	}
	
	public final E getEntry(int id) {
		try {
			return tryGetEntry(id);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public final E tryGetEntry(int id) throws IOException {
		int fo = getFileOffset(id);
		long offset;
		int len;
		if ( fo > 0 && maxIdList.get(fo-1) == id ) {
			offset = 0;
			len = (int)posList.getLong(id+1);
		}
		else {
			offset = posList.getLong(id);
			len = (int)(posList.getLong(id+1) - posList.getLong(id));
		}
		rafList.get(fo).seek(offset);
		rafList.get(fo).read(buffer, 0, len);
//		int[] tokens = Snappy.uncompressIntArray(buffer, 0, len);
		E entry = deserialize(buffer, 0, len);
		return entry;
	}
	
	private final int getFileOffset(int id) {
		for ( int fo=0; fo<maxIdList.size(); ++fo ) {
			if ( id < maxIdList.getInt(fo) ) return fo;
		}
		Exception e = new Exception("UNEXPECTED_ERROR#001");
		e.printStackTrace();
		System.exit(1);
		return 0;
	}
	
	public final Iterable<E> getEntries() {
		return new Iterable<E>() {
			
			@Override
			public Iterator<E> iterator() {
				return new EntryIterator();
			}
		};
	}
	
	public final int size() { return numEntries; }
	
	public final BigInteger diskSpaceUsage() {
		return FileUtils.sizeOfAsBigInteger(new File(path));
	}
	
	public final void printDetailStats() {
		StringBuilder strbld = new StringBuilder("EntryStore {\n");
		strbld.append("\tMAX_STORE_SIZE="+String.format("%,d", MAX_STORE_SIZE)+"\n");
		strbld.append("\tMAX_FILE_SIZE="+String.format("%,d", MAX_FILE_SIZE)+"\n");
		strbld.append("\tmaxIdList="+maxIdList+"\n");
		strbld.append("\trafList.size="+rafList.size()+"\n");
		strbld.append("\tposList[:10]="+posList.subList(0, Math.min(10, posList.size()))+"\n");
		strbld.append("\tpath="+path+"\n");
		strbld.append("\tnumEntries="+numEntries+"\n");
		strbld.append("\tstoreSize="+String.format("%,d", storeSize)+"\n");
		strbld.append("}");
		System.err.println(strbld.toString());
	}
	
	final class EntryIterator implements Iterator<E> {
		
		int i = 0;
		
		@Override
		public boolean hasNext() {
			return (i < numEntries);
		}

		@Override
		public E next() {
			E entry = null;
			try {
				entry = tryGetEntry(i);
			} catch (IOException e) {
				e.printStackTrace();
			}
			i += 1;
			return entry;
		}
	}

	private final class Cursor {
		int fileOffset;
		long offset;
		FileOutputStream fos = null;
		int numEntries;
		long storeSize;
		
		public Cursor() throws FileNotFoundException {
			open();
		}
		
		public final void open() throws FileNotFoundException {
			fos = new FileOutputStream(path+"."+fileOffset);
		}
		
		public final void write(byte[] b) throws IOException {
			offset += b.length;
			fos.write(b);
			numEntries += 1;
			storeSize += b.length;
		}
		
		public final void close() throws IOException {
			fos.flush();
			fos.close();
			maxIdList.add(numEntries);
			rafList.add(new RandomAccessFile(new File(path+"."+fileOffset), "r"));
		}

		public final void updateCursorIfNecessary() throws IOException {
			if ( offset > MAX_FILE_SIZE ) {
				close();
				fileOffset += 1;
				offset = 0;
				open();
			}
		}

		public final void checkStoreSizeOverflow() throws IOException {
			if ( storeSize > MAX_STORE_SIZE ) {
				Log.log.error(String.format("EntryStore size is %d which is larger than MAX_STORE_SIZE %d.", storeSize, MAX_STORE_SIZE));
				close();
				deleteAllFiles();
				System.exit(1);
			}
		}
	
		
		private final void deleteAllFiles() {
			for ( int i=0; i<fileOffset; ++i ) FileUtils.deleteQuietly(new File(path+"."+i));
		}
	}
}

package snu.kdd.substring_syn.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.apache.commons.io.FileUtils;

public class FileBasedLongList {

	private final int nMax = 1024;
	private final byte[] bytes = new byte[nMax*Long.BYTES];
	private final ByteBuffer buf;
	private final String path;
	private BufferedOutputStream bos;
	private RandomAccessFile raf;
	private int i0 = -1;
	private int size = 0;
	
	public FileBasedLongList() {
		this("FileBasedLongList");
	}
	
	public FileBasedLongList(String name) {
		buf = ByteBuffer.wrap(bytes);
		path = "./tmp/"+name;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(path));
			raf = new RandomAccessFile(path, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public final void add(long value) {
		buf.putLong(0, value);
		try {
			bos.write(bytes, 0, Long.BYTES);
			size += 1;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public final void finalize() {
		try {
			bos.flush();
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public final long get(int i) {
		if ( i >= size ) {
			IllegalAccessError e = new IllegalAccessError("index i="+i+" must be smaller than size="+size+".");
			e.printStackTrace();
			System.exit(1);
		}
		if ( i0 == -1 || i0 + nMax <= i || i < i0 ) readFromFile(i);
		return buf.getLong((i - i0)*Long.BYTES);
	}
	
	private final void readFromFile(int i) {
		try {
			raf.seek(i*Long.BYTES);
			raf.read(bytes);
			i0 = i;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public final int size() {
		return size;
	}
	
	public final BigInteger diskSpaceUsage() {
		return FileUtils.sizeOfAsBigInteger(new File(path));
	}
}

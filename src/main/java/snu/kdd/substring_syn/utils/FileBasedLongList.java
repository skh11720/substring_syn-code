package snu.kdd.substring_syn.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.apache.commons.io.FileUtils;

public class FileBasedLongList {

	private final byte[] bytes = new byte[Long.BYTES];
	private final ByteBuffer buf;
	private final String path;
	private FileOutputStream fos;
	private RandomAccessFile raf;
	private int size = 0;
	
	public FileBasedLongList() {
		this("FileBasedLongList");
	}
	
	public FileBasedLongList(String name) {
		buf = ByteBuffer.wrap(bytes);
		path = "./tmp/"+name;
		try {
			fos = new FileOutputStream(path);
			raf = new RandomAccessFile(path, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public final void add(long value) {
		buf.putLong(0, value);
		try {
			fos.write(bytes);
			++size;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public final long get(int i) {
		try {
			raf.seek(i*Long.BYTES);
			raf.read(bytes);
			return buf.getLong(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return 0;
	}
	
	public final int size() {
		return size;
	}
	
	public final BigInteger diskSpaceUsage() {
		return FileUtils.sizeOfAsBigInteger(new File(path));
	}
}

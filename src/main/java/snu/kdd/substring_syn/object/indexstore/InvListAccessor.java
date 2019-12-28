package snu.kdd.substring_syn.object.indexstore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;

import org.apache.commons.io.FileUtils;
import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class InvListAccessor {

	private final Int2ObjectMap<SegmentInfo> key2segMap;
	private final byte[] buffer;
	private final RandomAccessFile[] rafList;
	final BigInteger diskSpaceUsage;
	
	public InvListAccessor( String path, Int2ObjectMap<SegmentInfo> key2segMap, int nFiles, int bufSize ) throws FileNotFoundException {
		this.key2segMap = key2segMap;
		buffer = new byte[bufSize];
		rafList = new RandomAccessFile[nFiles];
		for (int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(path+"."+i, "r");
		diskSpaceUsage = computeDiskSpaceUsage(path, nFiles);
	}
	
	private final BigInteger computeDiskSpaceUsage(String path, int nFiles) {
		BigInteger sum = BigInteger.ZERO;
		for (int i=0; i<rafList.length; ++i ) {
			sum = sum.add(FileUtils.sizeOfAsBigInteger(new File(path+"."+i)));
		}
		return sum;
	}

	public final IntList getList( int key ) {
		SegmentInfo seg = key2segMap.get(key);
		if ( seg == null ) return null;
		int[] arr = null;
		try {
			rafList[seg.fileOffset].seek(seg.offset);
			rafList[seg.fileOffset].read(buffer, 0, seg.len);
			arr = Snappy.uncompressIntArray(buffer, 0, seg.len);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return IntArrayList.wrap(arr);
	}
}

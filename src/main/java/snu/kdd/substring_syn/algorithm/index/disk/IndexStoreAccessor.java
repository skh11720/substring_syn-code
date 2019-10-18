package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class IndexStoreAccessor {

	private final Int2ObjectMap<SegmentInfo> tok2segMap;
	private final byte[] buffer;
	private final RandomAccessFile[] rafList;
	public final long size;
	
	public IndexStoreAccessor( String path, Int2ObjectMap<SegmentInfo> tok2segMap, int nFiles, int bufSize, long size ) throws FileNotFoundException {
		this.tok2segMap = tok2segMap;
		buffer = new byte[bufSize];
		rafList = new RandomAccessFile[nFiles];
		for (int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(path+"."+i, "r");
		this.size = size;
	}

	public int[] getList( int token ) {
		SegmentInfo seg = tok2segMap.get(token);
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
		return arr;
	}
}

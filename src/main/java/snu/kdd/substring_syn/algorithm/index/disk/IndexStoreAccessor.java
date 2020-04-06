package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class IndexStoreAccessor {

	final String path;
	final Int2ObjectMap<ChunkSegmentInfo> tok2segMap;
	private final byte[] buffer;
	private final RandomAccessFile[] rafList;
	public final long size;
	public int[] arr = new int[1024];
	
	public IndexStoreAccessor( String path, Int2ObjectMap<ChunkSegmentInfo> tok2segMap, int nFiles, int bufSize, long size ) throws FileNotFoundException {
		this.path = path;
		this.tok2segMap = tok2segMap;
		buffer = new byte[bufSize];
		rafList = new RandomAccessFile[nFiles];
		for (int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(path+"."+i, "r");
		this.size = size;
	}

	public int getList( int token ) {
		ChunkSegmentInfo seg = tok2segMap.get(token);
		if ( seg == null ) return 0;
		try {
			rafList[seg.fileOffset].seek(seg.offset);
			rafList[seg.fileOffset].read(buffer, 0, seg.chunkLenList.getInt(0));
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		
		int bytes = 0;
		try {
			bytes = Snappy.uncompressedLength(buffer, 0, seg.chunkLenList.getInt(0));
			while (arr.length < bytes/Integer.BYTES) doubleBuffer();
			bytes = Snappy.rawUncompress(buffer, 0, seg.chunkLenList.getInt(0), arr, 0);
			
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return bytes/Integer.BYTES;
	}
	
	private void doubleBuffer() {
		arr = new int[2*arr.length];
	}
}

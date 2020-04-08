package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class IndexStoreAccessor {

	final String path;
	final Int2ObjectMap<ChunkSegmentInfo> tok2segMap;
	private final RandomAccessFile[] rafList;
	public final long size;
	public int[] arr = new int[1024];
	
	public IndexStoreAccessor( String path, Int2ObjectMap<ChunkSegmentInfo> tok2segMap, int nFiles, int bufSize, long size ) throws FileNotFoundException {
		this.path = path;
		this.tok2segMap = tok2segMap;
		rafList = new RandomAccessFile[nFiles];
		for (int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(path+"."+i, "r");
		this.size = size;
	}
	
	public PostingListAccessor getPostingListAccessor( int token ) {
		if ( tok2segMap.containsKey(token) ) return new PostingListAccessor(this, token);
		else return null;
	}
}

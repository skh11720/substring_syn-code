package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xerial.snappy.Snappy;

public class PostingListAccessor {

	public static int MAX_BYTES_PER_CHUNK = Snappy.maxCompressedLength(AbstractIndexStoreBuilder.MAX_NUM_INT_PER_CHUNK*Integer.BYTES);
	public static final int ADDITIONAL_SPACE = 3;
	private final IndexStoreAccessor parent;
	private final byte[] bbuf;
	private final int[] ibuf;
	private ChunkSegmentInfo cseg; 
	private RandomAccessFile raf = null;
	private long offset;
	private int chunkLenListIdx;
	private int chunkSize;
	
	PostingListAccessor( IndexStoreAccessor parent ) {
		this.parent = parent;
		bbuf = new byte[MAX_BYTES_PER_CHUNK];
		ibuf = new int[AbstractIndexStoreBuilder.MAX_NUM_INT_PER_CHUNK+ADDITIONAL_SPACE];
//		this.size = csegInfo.numInts;
	}

	PostingListAccessor( IndexStoreAccessor parent, int token ) {
		this.parent = parent;
		bbuf = new byte[MAX_BYTES_PER_CHUNK];
		ibuf = new int[AbstractIndexStoreBuilder.MAX_NUM_INT_PER_CHUNK+ADDITIONAL_SPACE];
		init(token);
	}
	
	public boolean init(int token) {
		if ( raf != null ) {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		cseg = parent.tok2segMap.get(token);
		if ( cseg != null ) {
			try {
				raf = new RandomAccessFile(parent.path+"."+cseg.fileOffset, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
			reset();
		}
		return (cseg != null);
	}
	
	public void reset() {
		offset = cseg.offset;
		chunkLenListIdx = 0;
	}
	
	public boolean hasNextChunk() {
		return chunkLenListIdx < cseg.chunkLenList.size();
	}

	public int nextChunk() {
		return nextChunk(0);
	}

	public int nextChunk(int remainder) {
		if ( remainder > 0 ) {
			for ( int i=0; i<remainder; ++i ) ibuf[i] = ibuf[chunkSize-remainder+i];
		}
		int len = cseg.chunkLenList.getInt(chunkLenListIdx);
		int bytes = 0;
		try {
			raf.seek(offset);
			raf.read(bbuf, 0, len);
			bytes = Snappy.rawUncompress(bbuf, 0, len, ibuf, Integer.BYTES*remainder);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		offset += len; 
		chunkLenListIdx += 1;

		chunkSize = bytes/Integer.BYTES + remainder;
		return chunkSize;
	}
	
	public int[] getIBuf() {
		return ibuf;
	}
	
	public int bytes() {
		return cseg.chunkLenList.stream().mapToInt(x->x).sum();
	}
	
	public int numInts() {
		return cseg.numInts;
	}
	
	public int chunkSize() {
		return chunkSize;
	}
}

package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xerial.snappy.Snappy;

public class PostingListAccessor {

	public static final int MAX_BYTES_PER_CHUNK = Snappy.maxCompressedLength(AbstractIndexStoreBuilder.MAX_NUM_INT_PER_CHUNK*Integer.BYTES);
	private final IndexStoreAccessor parent;
	private final byte[] bbuf;
	private final int[] ibuf;
	private ChunkSegmentInfo cseg; 
	private RandomAccessFile raf = null;
	private long offset;
	private int chunkLenListIdx;
	
	PostingListAccessor( IndexStoreAccessor parent ) {
		this.parent = parent;
		bbuf = new byte[MAX_BYTES_PER_CHUNK];
		ibuf = new int[AbstractIndexStoreBuilder.MAX_NUM_INT_PER_CHUNK];
//		this.size = csegInfo.numInts;
	}

	PostingListAccessor( IndexStoreAccessor parent, int token ) {
		this.parent = parent;
		bbuf = new byte[MAX_BYTES_PER_CHUNK];
		ibuf = new int[AbstractIndexStoreBuilder.MAX_NUM_INT_PER_CHUNK];
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
		return chunkLenListIdx < cseg.chunkLenList.size()-1;
	}

	public int nextChunk() {
		int len = cseg.chunkLenList.getInt(chunkLenListIdx);
		int bytes = 0;
		try {
			raf.seek(offset);
			raf.read(bbuf, 0, len);
			bytes = Snappy.rawUncompress(bbuf, 0, len, ibuf, 0);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		offset += len; 
		chunkLenListIdx += 1;

		return bytes/Integer.BYTES;
	}
	
	public int[] getIBuf() {
		return ibuf;
	}
	
	public int bytes() {
		return cseg.chunkLenList.stream().mapToInt(x->x).sum();
	}
}

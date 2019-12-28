package snu.kdd.substring_syn.object.indexstore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntPair;

public class IndexBuilder {

	static final int INMEM_MAX_SIZE = 16 * 1024 * 1024;
	protected static final long FILE_MAX_LEN = 8_000_000_000_000_000_000L;
	protected final Iterable<IntPair> kvList;
	protected FileOutputStream fos = null;
	protected int inmem_max_size;
	protected int nFlush;
	protected int bufSize;
	protected Cursor curTmp;
	protected Cursor curOut;
	
	
	IndexBuilder( Iterable<IntPair> kvList ) {
		this.kvList = kvList;
		inmem_max_size = INMEM_MAX_SIZE;
	}
	
	final void setInMemMaxSize( int inmem_max_size ) {
		this.inmem_max_size = inmem_max_size;
	}
	
	final int getNFlush() {
		return nFlush;
	}
	
	@FunctionalInterface
	interface ListSegmentBuilder {
		Int2ObjectMap<ObjectList<SegmentInfo>> build(Iterable<IntPair> kvList) throws IOException;
	}
	
	final InvListAccessor buildInvList() {
		return createIndexStoreAccessor(kvList, getInvPath(), this::buildInvListSegment);
	}

	final String getIndexStoreName() { return "IndexBuilder"; }
	
	final InvListAccessor createIndexStoreAccessor( Iterable<IntPair> kvList, String path, ListSegmentBuilder listSegmentBuilder ) {
		bufSize = nFlush = 0;
		curTmp = new Cursor();
		curOut = new Cursor();
		InvListAccessor accessor = null;
		try {
			Int2ObjectMap<ObjectList<SegmentInfo>> key2segList = listSegmentBuilder.build(kvList);
			Int2ObjectMap<SegmentInfo> key2SegMap = mergeSegments(path, key2segList);
			accessor = new InvListAccessor(path, key2SegMap, curOut.fileOffset+1, bufSize);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return accessor;
	}
	
	final Int2ObjectMap<ObjectList<SegmentInfo>> buildInvListSegment( Iterable<IntPair> kvList ) throws IOException {
		Int2ObjectMap<IntList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> key2segList = new Int2ObjectOpenHashMap<>();
		openNextTmpFile();
		int size = 0;
		for ( IntPair entry : kvList ) {
			int key = entry.i1;
			int id = entry.i2;
			if ( !invListMap.containsKey(key) ) invListMap.put(key, new IntArrayList());
			invListMap.get(key).add(id);
			size += 1;
			if ( size >= inmem_max_size ) {
				flushInmemMap(invListMap, key2segList);
				invListMap = new Int2ObjectOpenHashMap<>();
				size = 0;
			}
		}
		flushInmemMap(invListMap, key2segList);
		fos.close();
		return key2segList;
	}
	
	final void flushInmemMap( Int2ObjectMap<IntList> invListMap, Int2ObjectMap<ObjectList<SegmentInfo>> key2segList ) throws IOException {
		for ( Int2ObjectMap.Entry<IntList> entry : invListMap.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			int[] arr = entry.getValue().toIntArray();
			byte[] b = Snappy.compress(arr);
			if ( !key2segList.containsKey(token) ) key2segList.put(token, new ObjectArrayList<>());
			
			if ( curTmp.offset > FILE_MAX_LEN ) openNextTmpFile();

			key2segList.get(token).add(new SegmentInfo(curTmp.fileOffset, curTmp.offset, b.length));
			fos.write(b);
			curTmp.offset += b.length;
			bufSize = Math.max(bufSize, b.length);
		}
		++nFlush;
	}
	
	final Int2ObjectMap<SegmentInfo> mergeSegments( String path, Int2ObjectMap<ObjectList<SegmentInfo>> key2segList ) throws IOException {
		Int2ObjectMap<SegmentInfo> key2segMap = new Int2ObjectOpenHashMap<>();
		RandomAccessFile[] rafList = new RandomAccessFile[curTmp.fileOffset+1];
		for ( int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(getTmpPath(i), "r");
		FileOutputStream fos = new FileOutputStream(path+"."+curOut.fileOffset);
		byte[] buffer = new byte[bufSize];
		for ( Int2ObjectMap.Entry<ObjectList<SegmentInfo>> entry : key2segList.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			ObjectList<SegmentInfo> segList = entry.getValue();
			IntList invList = new IntArrayList();
			for ( SegmentInfo seg : segList ) {
				rafList[seg.fileOffset].seek(seg.offset);
				rafList[seg.fileOffset].read(buffer, 0, seg.len);
				invList.addAll(IntArrayList.wrap(Snappy.uncompressIntArray(buffer, 0, seg.len)));
			}
			byte[] b = Snappy.compress(invList.toIntArray());
			bufSize = Math.max(bufSize, b.length);

			if ( curOut.offset > FILE_MAX_LEN ) {
				fos.flush();
				fos.close();
				curOut.fileOffset += 1;
				curOut.offset = 0;
				fos = new FileOutputStream(path+"."+curOut.fileOffset);
			}

			key2segMap.put(token, new SegmentInfo(curOut.fileOffset, curOut.offset, b.length));
			fos.write(b);
			curOut.offset += b.length;
		}
		for ( int i=0; i<rafList.length; ++i ) rafList[i].close();
		fos.close();
		return key2segMap;
	}
	
	final void openNextTmpFile() throws IOException {
		if ( fos != null ) {
			fos.flush();
			fos.close();
			curTmp.fileOffset += 1;
			curTmp.offset = 0;
		}
		fos = new FileOutputStream(getTmpPath(curTmp.fileOffset));
	}

	final String getInvPath() { return "./tmp/"+getIndexStoreName()+".inv"; }

	final String getTmpPath( int fileOffset ) { return "./tmp/"+getIndexStoreName()+".tmp."+fileOffset; }
	


	private class Cursor {
		int fileOffset;
		long offset;
	}
}

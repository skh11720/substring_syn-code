package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;

public abstract class AbstractIndexStoreBuilder {

	public static final int INMEM_MAX_SIZE = 16 * 1024 * 1024;
	protected static final long FILE_MAX_LEN = 8_000_000_000_000_000_000L;
	protected final Iterable<TransformableRecordInterface> recordList;
	protected BufferedOutputStream bos = null;
	protected int inmem_max_size;
	protected int nFlush;
	protected int bufSize;
	protected long storeSize;
	protected Cursor curTmp;
	protected Cursor curOut;
	protected int[] ibuf = new int[1024];
	protected byte[] bbuf = new byte[1024];
	

	public AbstractIndexStoreBuilder( Iterable<TransformableRecordInterface> recordList ) {
		this.recordList = recordList;
		inmem_max_size = INMEM_MAX_SIZE;
	}
	
	public final void setInMemMaxSize( int inmem_max_size ) {
		this.inmem_max_size = inmem_max_size;
	}
	
	public final int getNFlush() {
		return nFlush;
	}
	
	@FunctionalInterface
	protected interface ListSegmentBuilder {
		Int2ObjectMap<ObjectList<SegmentInfo>> build(Iterable<TransformableRecordInterface> recordList) throws IOException;
	}
	
	public abstract IndexStoreAccessor buildInvList();

	public abstract IndexStoreAccessor buildTrInvList();
	
	protected abstract void addToInvList( IntArrayList list, TransformableRecordInterface rec, int pos );

	protected abstract void addToTrInvList( IntArrayList list, TransformableRecordInterface rec, int pos, Rule rule );

	protected abstract int invListEntrySize();

	protected abstract int trInvListEntrySize();
	
	protected abstract String getIndexStoreName();
	
	protected IndexStoreAccessor createIndexStoreAccessor( Iterable<TransformableRecordInterface> recordList, String path, ListSegmentBuilder listSegmentBuilder ) {
		bufSize = nFlush = 0;
		storeSize = 0;
		curTmp = new Cursor();
		curOut = new Cursor();
		IndexStoreAccessor accessor = null;
		try {
			Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = listSegmentBuilder.build(recordList);
			Int2ObjectMap<SegmentInfo> tok2SegMap = mergeSegments(path, tok2segList);
			accessor = new IndexStoreAccessor(path, tok2SegMap, curOut.fileOffset+1, bufSize, storeSize);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return accessor;
	}
	
	protected Int2ObjectMap<ObjectList<SegmentInfo>> buildInvListSegment( Iterable<TransformableRecordInterface> recordList ) throws IOException {
		Int2ObjectMap<IntArrayList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		openNextTmpFile();
		int size = 0;
		for ( TransformableRecordInterface rec : recordList ) {
			for ( int i=0; i<rec.size(); ++i ) {
				int token = rec.getToken(i);
				if ( !invListMap.containsKey(token) ) invListMap.put(token, new IntArrayList());
				addToInvList(invListMap.get(token), rec, i);
			}
			size += rec.size();
			if ( size >= inmem_max_size ) {
				flushInmemMap(invListMap, tok2segList);
				invListMap = new Int2ObjectOpenHashMap<>();
				size = 0;
			}
		}
		flushInmemMap(invListMap, tok2segList);
		bos.close();
		return tok2segList;
	}
	
	protected Int2ObjectMap<ObjectList<SegmentInfo>> buildTrInvListSegment( Iterable<TransformableRecordInterface> recordList ) throws IOException {
		Int2ObjectMap<IntArrayList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		openNextTmpFile();
		int size = 0;
		for ( TransformableRecordInterface rec : recordList ) {
			rec.preprocessApplicableRules();
			for ( int i=0; i<rec.size(); ++i ) {
				for ( Rule rule : rec.getApplicableRules(i) ) {
					if ( rule.isSelfRule ) continue;
					for ( int token : rule.getRhs() ) {
						if ( !invListMap.containsKey(token) ) invListMap.put(token, new IntArrayList());
						addToTrInvList(invListMap.get(token), rec, i, rule);
					}
				size += rule.rhsSize();
				}
			}

			if ( size >= inmem_max_size ) {
				flushInmemMap(invListMap, tok2segList);
				invListMap = new Int2ObjectOpenHashMap<>();
				size = 0;
			}
		}
		flushInmemMap(invListMap, tok2segList);
		bos.close();
		return tok2segList;
	}

	protected void flushInmemMap( Int2ObjectMap<IntArrayList> invListMap, Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList ) throws IOException {
		for ( Int2ObjectMap.Entry<IntArrayList> entry : invListMap.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			IntArrayList invList = entry.getValue();
			int[] arr = entry.getValue().elements();
			int blenMax = Snappy.maxCompressedLength(invList.size()*Integer.BYTES);
			while ( blenMax > bbuf.length ) doubleByteBuffer();
			int blen = Snappy.rawCompress(arr, 0, invList.size()*Integer.BYTES, bbuf, 0);
			if ( !tok2segList.containsKey(token) ) tok2segList.put(token, new ObjectArrayList<>());
			
			if ( curTmp.offset > FILE_MAX_LEN ) openNextTmpFile();

			tok2segList.get(token).add(new SegmentInfo(curTmp.fileOffset, curTmp.offset, blen));
			bos.write(bbuf, 0, blen);
			curTmp.offset += blen;
			bufSize = Math.max(bufSize, blen);
		}
		++nFlush;
	}
	
	static int orderOfCalls = 0;
	protected Int2ObjectMap<SegmentInfo> mergeSegments( String path, Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList ) throws IOException {
		Int2ObjectMap<SegmentInfo> tok2segMap = new Int2ObjectOpenHashMap<>();
		RandomAccessFile[] rafList = new RandomAccessFile[curTmp.fileOffset+1];
		for ( int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(getTmpPath(i), "r");
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path+"."+curOut.fileOffset));
		byte[] buffer = new byte[bufSize];
		for ( Int2ObjectMap.Entry<ObjectList<SegmentInfo>> entry : tok2segList.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			ObjectList<SegmentInfo> segList = entry.getValue();
			IntArrayList invList = new IntArrayList();
			for ( SegmentInfo seg : segList ) {
				rafList[seg.fileOffset].seek(seg.offset);
				rafList[seg.fileOffset].read(buffer, 0, seg.len);
				int bytes = Snappy.uncompressedLength(buffer, 0, seg.len);
				while (ibuf.length < bytes/Integer.BYTES) doubleIntBuffer();
				bytes = Snappy.rawUncompress(buffer, 0, seg.len, ibuf, 0);
				invList.addAll(IntArrayList.wrap(ibuf, bytes/Integer.BYTES));
			}
			int blenMax = Snappy.maxCompressedLength(invList.size()*Integer.BYTES);
			while ( blenMax > bbuf.length ) doubleByteBuffer();
			Log.log.trace("mergeSegments_%d  token=%d, invList.size=%d", orderOfCalls, token, invList.size());
			int blen = Snappy.rawCompress(invList.elements(), 0, invList.size()*Integer.BYTES, bbuf, 0);
			bufSize = Math.max(bufSize, blen);

			if ( curOut.offset > FILE_MAX_LEN ) {
				bos.flush();
				bos.close();
				curOut.fileOffset += 1;
				curOut.offset = 0;
				bos = new BufferedOutputStream(new FileOutputStream(path+"."+curOut.fileOffset));
			}

			tok2segMap.put(token, new SegmentInfo(curOut.fileOffset, curOut.offset, blen));
			bos.write(bbuf, 0, blen);
			curOut.offset += blen;
			storeSize += blen;
		}
		for ( int i=0; i<rafList.length; ++i ) rafList[i].close();
		bos.close();
		orderOfCalls += 1;
		return tok2segMap;
	}
	
	protected void openNextTmpFile() throws IOException {
		if ( bos != null ) {
			bos.flush();
			bos.close();
			curTmp.fileOffset += 1;
			curTmp.offset = 0;
		}
		bos = new BufferedOutputStream(new FileOutputStream(getTmpPath(curTmp.fileOffset)));
	}

	protected String getInvPath() { return "./tmp/"+getIndexStoreName()+".inv"; }

	protected String getTinvPath() { return "./tmp/"+getIndexStoreName()+".tinv"; }

	protected String getTmpPath( int fileOffset ) { return "./tmp/"+getIndexStoreName()+".tmp."+fileOffset; }
	
	public final long diskSpaceUsage() { return storeSize; }

	private void doubleIntBuffer() {
		ibuf = new int[2*ibuf.length];
	}

	private void doubleByteBuffer() {
		bbuf = new byte[2*bbuf.length];
	}

	private class Cursor {
		int fileOffset;
		long offset;
	}
}

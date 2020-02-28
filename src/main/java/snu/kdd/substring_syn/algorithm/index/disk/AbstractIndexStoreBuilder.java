package snu.kdd.substring_syn.algorithm.index.disk;

import java.io.BufferedOutputStream;
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
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;

public abstract class AbstractIndexStoreBuilder {

	public static final int INMEM_MAX_SIZE = 16 * 1024 * 1024;
	protected static final long FILE_MAX_LEN = 8_000_000_000_000_000_000L;
	protected final Iterable<Record> recordList;
	protected BufferedOutputStream bos = null;
	protected int inmem_max_size;
	protected int nFlush;
	protected int bufSize;
	protected long storeSize;
	protected Cursor curTmp;
	protected Cursor curOut;
	protected int[] ibuf = new int[1024*1024];
	

	public AbstractIndexStoreBuilder( Iterable<Record> recordList ) {
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
		Int2ObjectMap<ObjectList<SegmentInfo>> build(Iterable<Record> recordList) throws IOException;
	}
	
	public abstract IndexStoreAccessor buildInvList();

	public abstract IndexStoreAccessor buildTrInvList();
	
	protected abstract void addToInvList( IntList list, Record rec, int pos );

	protected abstract void addToTrInvList( IntList list, Record rec, int pos, Rule rule );

	protected abstract String getIndexStoreName();
	
	protected IndexStoreAccessor createIndexStoreAccessor( Iterable<Record> recordList, String path, ListSegmentBuilder listSegmentBuilder ) {
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
	
	protected Int2ObjectMap<ObjectList<SegmentInfo>> buildInvListSegment( Iterable<Record> recordList ) throws IOException {
		Int2ObjectMap<IntList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		openNextTmpFile();
		int size = 0;
		for ( Record rec : recordList ) {
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
	
	protected Int2ObjectMap<ObjectList<SegmentInfo>> buildTrInvListSegment( Iterable<Record> recordList ) throws IOException {
		Int2ObjectMap<IntList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		openNextTmpFile();
		int size = 0;
		for ( Record rec : recordList ) {
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

	protected void flushInmemMap( Int2ObjectMap<IntList> invListMap, Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList ) throws IOException {
		for ( Int2ObjectMap.Entry<IntList> entry : invListMap.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			int[] arr = entry.getValue().toIntArray();
			byte[] b = Snappy.rawCompress(arr, arr.length*Integer.BYTES);
			if ( !tok2segList.containsKey(token) ) tok2segList.put(token, new ObjectArrayList<>());
			
			if ( curTmp.offset > FILE_MAX_LEN ) openNextTmpFile();

			tok2segList.get(token).add(new SegmentInfo(curTmp.fileOffset, curTmp.offset, b.length));
			bos.write(b);
			curTmp.offset += b.length;
			bufSize = Math.max(bufSize, b.length);
		}
		++nFlush;
	}
	
	protected Int2ObjectMap<SegmentInfo> mergeSegments( String path, Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList ) throws IOException {
		Int2ObjectMap<SegmentInfo> tok2segMap = new Int2ObjectOpenHashMap<>();
		RandomAccessFile[] rafList = new RandomAccessFile[curTmp.fileOffset+1];
		for ( int i=0; i<rafList.length; ++i ) rafList[i] = new RandomAccessFile(getTmpPath(i), "r");
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path+"."+curOut.fileOffset));
		byte[] buffer = new byte[bufSize];
		for ( Int2ObjectMap.Entry<ObjectList<SegmentInfo>> entry : tok2segList.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			ObjectList<SegmentInfo> segList = entry.getValue();
			IntList invList = new IntArrayList();
			for ( SegmentInfo seg : segList ) {
				rafList[seg.fileOffset].seek(seg.offset);
				rafList[seg.fileOffset].read(buffer, 0, seg.len);
				int bytes = Snappy.uncompressedLength(buffer, 0, seg.len);
				while (ibuf.length < bytes/Integer.BYTES) doubleBuffer();
				bytes = Snappy.rawUncompress(buffer, 0, seg.len, ibuf, 0);
				invList.addAll(IntArrayList.wrap(ibuf, bytes/Integer.BYTES));
			}
			byte[] b = Snappy.rawCompress(invList.toIntArray(), invList.size()*Integer.BYTES);
			bufSize = Math.max(bufSize, b.length);

			if ( curOut.offset > FILE_MAX_LEN ) {
				bos.flush();
				bos.close();
				curOut.fileOffset += 1;
				curOut.offset = 0;
				bos = new BufferedOutputStream(new FileOutputStream(path+"."+curOut.fileOffset));
			}

			tok2segMap.put(token, new SegmentInfo(curOut.fileOffset, curOut.offset, b.length));
			bos.write(b);
			curOut.offset += b.length;
			storeSize += b.length;
		}
		for ( int i=0; i<rafList.length; ++i ) rafList[i].close();
		bos.close();
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

	private void doubleBuffer() {
		ibuf = new int[2*ibuf.length];
	}

	private class Cursor {
		int fileOffset;
		long offset;
	}
}

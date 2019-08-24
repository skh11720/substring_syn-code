package snu.kdd.substring_syn.algorithm.index.disk;

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

public class NaiveIndexStoreBuilder {

	static final int INMEM_MAX_SIZE = 16 * 1024 * 1024;
	static final String invPath = "./tmp/NaiveIndexStore.inv";
	static final String tinvPath = "./tmp/NaiveIndexStore.tinv";
	static final String tmpPath = "./tmp/NaiveIndexStore.tmp";
	private final Iterable<Record> recordList;
	private FileOutputStream fos;
	private long offset;
	int inmem_max_size;
	int nFlush;
	int bufSize;
	long size;
	

	public NaiveIndexStoreBuilder( Iterable<Record> recordList ) {
		this.recordList = recordList;
		inmem_max_size = INMEM_MAX_SIZE;
	}
	
	public void setInMemMaxSize( int inmem_max_size ) {
		this.inmem_max_size = inmem_max_size;
	}
	
	public final int getNFlush() {
		return nFlush;
	}
	
	@FunctionalInterface
	interface ListSegmentBuilder {
		Int2ObjectMap<ObjectList<SegmentInfo>> build(Iterable<Record> recordList) throws IOException;
	}
	
	public IndexStoreAccessor buildInvList() {
		return createIndexStoreAccessor(recordList, invPath, this::buildInvListSegment);
	}

	public IndexStoreAccessor buildTrInvList() {
		return createIndexStoreAccessor(recordList, tinvPath, this::buildTrInvListSegment);
	}

	private IndexStoreAccessor createIndexStoreAccessor( Iterable<Record> recordList, String path, ListSegmentBuilder listSegmentBuilder ) {
		offset = bufSize = nFlush = 0;
		size = 0;
		IndexStoreAccessor accessor = null;
		try {
			Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = listSegmentBuilder.build(recordList);
			Int2ObjectMap<SegmentInfo> tok2SegMap = mergeSegments(invPath, tok2segList);
			accessor = new IndexStoreAccessor(invPath, tok2SegMap, bufSize, size);
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return accessor;
	}
	
	private Int2ObjectMap<ObjectList<SegmentInfo>> buildInvListSegment( Iterable<Record> recordList ) throws IOException {
		Int2ObjectMap<IntList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		fos = new FileOutputStream(tmpPath);
		int size = 0;
		for ( Record rec : recordList ) {
			for ( int i=0; i<rec.size(); ++i ) {
				int token = rec.getToken(i);
				if ( !invListMap.containsKey(token) ) invListMap.put(token, new IntArrayList());
				invListMap.get(token).add(rec.getID());
			}
			size += rec.size();
			if ( size >= inmem_max_size ) {
				flushInmemMap(invListMap, tok2segList);
				invListMap = new Int2ObjectOpenHashMap<>();
				size = 0;
			}
		}
		flushInmemMap(invListMap, tok2segList);
		fos.close();
		return tok2segList;
	}
	
	private Int2ObjectMap<ObjectList<SegmentInfo>> buildTrInvListSegment( Iterable<Record> recordList ) throws IOException {
		Int2ObjectMap<IntList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		fos = new FileOutputStream(tmpPath);
		int size = 0;
		for ( Record rec : recordList ) {
			rec.preprocessApplicableRules();
			for ( Rule rule : rec.getApplicableRuleIterable() ) {
				if ( rule.isSelfRule ) continue;
				for ( int token : rule.getRhs() ) {
					if ( !invListMap.containsKey(token) ) invListMap.put(token, new IntArrayList());
					invListMap.get(token).add(rec.getID());
				}
				size += rule.rhsSize();
			}

			if ( size >= inmem_max_size ) {
				flushInmemMap(invListMap, tok2segList);
				invListMap = new Int2ObjectOpenHashMap<>();
				size = 0;
			}
		}
		flushInmemMap(invListMap, tok2segList);
		fos.close();
		return tok2segList;
	}

	private void flushInmemMap( Int2ObjectMap<IntList> invListMap, Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList ) throws IOException {
		for ( Int2ObjectMap.Entry<IntList> entry : invListMap.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			int[] arr = entry.getValue().toIntArray();
			byte[] b = Snappy.compress(arr);
			if ( !tok2segList.containsKey(token) ) tok2segList.put(token, new ObjectArrayList<>());
			tok2segList.get(token).add(new SegmentInfo(offset, b.length));
			fos.write(b);
			offset += b.length;
			bufSize = Math.max(bufSize, b.length);
		}
		++nFlush;
	}
	
	private Int2ObjectMap<SegmentInfo> mergeSegments( String path, Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList ) throws IOException {
		Int2ObjectMap<SegmentInfo> tok2segMap = new Int2ObjectOpenHashMap<>();
		RandomAccessFile raf = new RandomAccessFile(tmpPath, "r");
		FileOutputStream fos = new FileOutputStream(path);
		byte[] buffer = new byte[bufSize];
		int cur = 0;
		for ( Int2ObjectMap.Entry<ObjectList<SegmentInfo>> entry : tok2segList.int2ObjectEntrySet() ) {
			int token = entry.getIntKey();
			ObjectList<SegmentInfo> segList = entry.getValue();
			IntList invList = new IntArrayList();
			for ( SegmentInfo seg : segList ) {
				raf.seek(seg.offset);
				raf.read(buffer, 0, seg.len);
				invList.addAll(IntArrayList.wrap(Snappy.uncompressIntArray(buffer, 0, seg.len)));
			}
			size += invList.size();
			byte[] b = Snappy.compress(invList.toIntArray());
			bufSize = Math.max(bufSize, b.length);
			tok2segMap.put(token, new SegmentInfo(cur, b.length));
			fos.write(b);
			cur += b.length;
		}
		raf.close();
		fos.close();
		return tok2segMap;
	}
}

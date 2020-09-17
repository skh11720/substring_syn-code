package snu.kdd.pkwise;

import java.io.IOException;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.AbstractIndexStoreBuilder;
import snu.kdd.substring_syn.algorithm.index.disk.IndexStoreAccessor;
import snu.kdd.substring_syn.algorithm.index.disk.SegmentInfo;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseQGramIndexStoreBuilder extends AbstractIndexStoreBuilder {
	
	public final String storeName;
	private final double theta;
	private final PkwiseSignatureGenerator siggen;

	public PkwiseQGramIndexStoreBuilder(Iterable<TransformableRecordInterface> recordList, double theta, PkwiseSignatureGenerator siggen ) {
		this(recordList, theta, siggen, "NaiveIndexStore");
	}

	public PkwiseQGramIndexStoreBuilder(Iterable<TransformableRecordInterface> recordList, double theta, PkwiseSignatureGenerator siggen, String storeName ) {
		super(recordList);
		this.theta = theta;
		this.siggen = siggen;
		this.storeName = storeName;
	}

	@Override
	public IndexStoreAccessor buildInvList() {
		return createIndexStoreAccessor(recordList, getInvPath(), this::buildInvListSegment);
	}

	@Override
	protected Int2ObjectMap<ObjectList<SegmentInfo>> buildInvListSegment( Iterable<TransformableRecordInterface> recordList ) throws IOException {
		Int2ObjectMap<IntArrayList> invListMap = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<ObjectList<SegmentInfo>> tok2segList = new Int2ObjectOpenHashMap<>();
		openNextTmpFile();
		int size = 0;
		int ridx = 0;
		int maxDiff = -1;
		int q = -1;
		for ( TransformableRecordInterface rec : recordList ) {
			Subrecord qgram = new Subrecord(rec, 1, rec.size());
			// Note that rec is an IntQGram: rec.arr[0] is the id and rec.arr[1:] is the actual qgram
			if ( q != qgram.size() ) {
				q = qgram.size();
				maxDiff = Util.getPrefixLength(qgram.size(), theta);
			}
			IntArrayList sig = siggen.genSignature(qgram, maxDiff, true);
			for ( int token : sig ) {
				if ( !invListMap.containsKey(token) ) invListMap.put(token, new IntArrayList());
				addQGramIdxToInvList(invListMap.get(token), ridx);
			}
			size += rec.size();
			if ( size >= inmem_max_size ) {
				flushInmemMap(invListMap, tok2segList);
				invListMap = new Int2ObjectOpenHashMap<>();
				size = 0;
			}
			ridx += 1;
		}
		flushInmemMap(invListMap, tok2segList);
		bos.close();
		return tok2segList;
	}
	
	protected void addQGramIdxToInvList( IntArrayList list, int ridx ) {
		list.add(ridx);
	}
	
	@Override
	public IndexStoreAccessor buildTrInvList() {
		return null;
	}

	@Override
	protected void addToInvList( IntArrayList list, TransformableRecordInterface rec, int pos ) {
	}

	@Override
	protected void addToTrInvList( IntArrayList list, TransformableRecordInterface rec, int pos, Rule rule ) {
	}

	@Override
	protected int invListEntrySize() {
		return 1;
	}

	@Override
	protected int trInvListEntrySize() {
		return 1;
	}

	@Override
	protected String getIndexStoreName() {
		return storeName;
	}
}

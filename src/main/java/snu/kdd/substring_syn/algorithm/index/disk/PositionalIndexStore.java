package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.BufferedPositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.BufferedPositionTrInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class PositionalIndexStore {

	final IndexStoreAccessor invListAccessor;
	final IndexStoreAccessor tinvListAccessor;
	
	public PositionalIndexStore( Iterable<TransformableRecordInterface> recordList ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE);
	}

	public PositionalIndexStore( Iterable<TransformableRecordInterface> recordList, int mem ) {
		PositionalIndexStoreBuilder builder = new PositionalIndexStoreBuilder(recordList);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		tinvListAccessor = builder.buildTrInvList();
	}

//	public PositionInvList getInvList( int token ) {
//        int length = invListAccessor.getList(token);
//        if ( length == 0 ) return null;
//        InmemPositionInvList list = new InmemPositionInvList(invListAccessor.arr, length/2);
//        Log.log.trace("PositionalIndexStore.getInvList: token=%d, list.size=%d", token, list.size);
//        for ( list.init(); list.hasNext(); list.next() ) Log.log.trace("PositionalIndexStore.getInvList: (%d, %d)", list.getIdx(), list.getPos());
//        return list;
//	}
//	
//	public PositionTrInvList getTrInvList( int token ) {
//        int length = tinvListAccessor.getList(token);
//        if ( length == 0 ) return null;
//        InmemPositionTrInvList list = new InmemPositionTrInvList(tinvListAccessor.arr, length/3);
//        Log.log.trace("PositionalIndexStore.getTrInvList: token=%d, list.size=%d", token, list.size);
//        for ( list.init(); list.hasNext(); list.next() ) Log.log.trace("PositionalIndexStore.getTrInvList: (%d, %d, %d)", list.getIdx(), list.getLeft(), list.getRight());
//        return list;
//	}

	public PositionInvList getInvList( int token ) {
		PostingListAccessor acc = invListAccessor.getPostingListAccessor(token);
		if ( acc == null ) return null;
		BufferedPositionInvList list = new BufferedPositionInvList(acc);
//        Log.log.trace("PositionalIndexStore.getInvList: token=%d, list.size=%d", token, list.size());
//        for ( list.init(); list.hasNext(); list.next() ) Log.log.trace("PositionalIndexStore.getInvList: (%d, %d)", list.getIdx(), list.getPos());
        return list;
	}
	
	public PositionTrInvList getTrInvList( int token ) {
		PostingListAccessor acc = tinvListAccessor.getPostingListAccessor(token);
		if ( acc == null ) return null;
		BufferedPositionTrInvList list = new BufferedPositionTrInvList(acc);
//        Log.log.trace("PositionalIndexStore.getTrInvList: token=%d, list.size=%d", token, list.size());
//        for ( list.init(); list.hasNext(); list.next() ) Log.log.trace("PositionalIndexStore.getTrInvList: (%d, %d, %d)", list.getIdx(), list.getLeft(), list.getRight());
        return list;
	}

	public final BigInteger diskSpaceUsage() {
		return new BigInteger(""+(invListAccessor.size + tinvListAccessor.size));
	}
}

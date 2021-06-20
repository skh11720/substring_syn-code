package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.BufferedPositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.BufferedPositionTrInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.PositionTrInvList;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class PositionalIndexStore {

	public final IndexStoreAccessor invListAccessor;
	public final IndexStoreAccessor tinvListAccessor;
	
	public PositionalIndexStore( Iterable<TransformableRecordInterface> recordList ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE);
	}

	public PositionalIndexStore( Iterable<TransformableRecordInterface> recordList, int mem ) {
		PositionalIndexStoreBuilder builder = new PositionalIndexStoreBuilder(recordList);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		tinvListAccessor = builder.buildTrInvList();
	}


	public PositionInvList getInvList( int token ) {
		PostingListAccessor acc = invListAccessor.getPostingListAccessor(token);
		if ( acc == null ) return null;
		BufferedPositionInvList list = new BufferedPositionInvList(acc);
        return list;
	}
	
	public PositionTrInvList getTrInvList( int token ) {
		PostingListAccessor acc = tinvListAccessor.getPostingListAccessor(token);
		if ( acc == null ) return null;
		BufferedPositionTrInvList list = new BufferedPositionTrInvList(acc);
        return list;
	}

	public final BigInteger diskSpaceUsage() {
		return new BigInteger(""+(invListAccessor.size + tinvListAccessor.size));
	}
}

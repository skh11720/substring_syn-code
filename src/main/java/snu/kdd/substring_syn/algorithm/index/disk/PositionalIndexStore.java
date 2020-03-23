package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

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

	public PositionInvList getInvList( int token ) {
		int length = invListAccessor.getList(token);
		if ( length == 0 ) return null;
		else return new PositionInvList(invListAccessor.arr, length/2);
	}
	
	public PositionTrInvList getTrInvList( int token ) {
		int length = tinvListAccessor.getList(token);
		if ( length == 0 ) return null;
		else return new PositionTrInvList(tinvListAccessor.arr, length/3);
	}

	public final BigInteger diskSpaceUsage() {
		return new BigInteger(""+(invListAccessor.size + tinvListAccessor.size));
	}
}

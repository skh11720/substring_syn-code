package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.BufferedNaiveInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class NaiveIndexStore {

	final IndexStoreAccessor invListAccessor;
	final IndexStoreAccessor tinvListAccessor;
	
	public NaiveIndexStore( Iterable<TransformableRecordInterface> recordList ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE);
	}

	public NaiveIndexStore( Iterable<TransformableRecordInterface> recordList, int mem ) {
		NaiveIndexStoreBuilder builder = new NaiveIndexStoreBuilder(recordList);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		tinvListAccessor = builder.buildTrInvList();
	}

	public NaiveInvList getInvList( int token ) {
		PostingListAccessor acc = invListAccessor.getPostingListAccessor(token);
		if ( acc == null ) return null;
		else return new BufferedNaiveInvList(acc);
	}

	public NaiveInvList getTrInvList( int token ) {
		PostingListAccessor acc = tinvListAccessor.getPostingListAccessor(token);
		if ( acc == null ) return null;
		else return new BufferedNaiveInvList(acc);
	}
	
	public final BigInteger diskSpaceUsage() {
		return new BigInteger(""+(invListAccessor.size + tinvListAccessor.size));
	}
}

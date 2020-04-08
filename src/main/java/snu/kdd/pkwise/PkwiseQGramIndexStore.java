package snu.kdd.pkwise;

import java.util.Arrays;

import snu.kdd.substring_syn.algorithm.index.disk.AbstractIndexStoreBuilder;
import snu.kdd.substring_syn.algorithm.index.disk.IndexStoreAccessor;
import snu.kdd.substring_syn.algorithm.index.disk.objects.InmemNaiveInvList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class PkwiseQGramIndexStore {

	final IndexStoreAccessor invListAccessor;
	public final long storeSize;
	
	public PkwiseQGramIndexStore( Iterable<TransformableRecordInterface> recordList, double theta, PkwiseSignatureGenerator siggen, String storeName ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE, theta, siggen, storeName);
	}

	public PkwiseQGramIndexStore( Iterable<TransformableRecordInterface> recordList, int mem, double theta, PkwiseSignatureGenerator siggen, String storeName ) {
		PkwiseQGramIndexStoreBuilder builder = new PkwiseQGramIndexStoreBuilder(recordList, theta, siggen, storeName);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		storeSize = builder.diskSpaceUsage();
	}

	public NaiveInvList getInvList( int token ) {
		int len = invListAccessor.getList(token);
		if ( invListAccessor.arr == null ) return null;
		else return new InmemNaiveInvList(Arrays.copyOf(invListAccessor.arr, len), len); // copying is necesary
	}
}

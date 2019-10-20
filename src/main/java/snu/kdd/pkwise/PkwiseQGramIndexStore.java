package snu.kdd.pkwise;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.algorithm.index.disk.AbstractIndexStoreBuilder;
import snu.kdd.substring_syn.algorithm.index.disk.IndexStoreAccessor;
import snu.kdd.substring_syn.data.record.Record;

public class PkwiseQGramIndexStore {

	final IndexStoreAccessor invListAccessor;
	
	public PkwiseQGramIndexStore( Iterable<Record> recordList, double theta, PkwiseSignatureGenerator siggen, String storeName ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE, theta, siggen, storeName);
	}

	public PkwiseQGramIndexStore( Iterable<Record> recordList, int mem, double theta, PkwiseSignatureGenerator siggen, String storeName ) {
		PkwiseQGramIndexStoreBuilder builder = new PkwiseQGramIndexStoreBuilder(recordList, theta, siggen, storeName);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
	}

	public IntList getInvList( int token ) {
		int[] arr = invListAccessor.getList(token);
		if ( arr == null ) return null;
		else return IntArrayList.wrap(arr);
	}
}

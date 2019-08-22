package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;

public class NaiveIndexStore {

	final IndexStoreAccessor invListAccessor;
	final IndexStoreAccessor tinvListAccessor;
	
	public NaiveIndexStore( Iterable<Record> recordList ) {
		this(recordList, NaiveIndexStoreBuilder.INMEM_MAX_SIZE);
	}

	public NaiveIndexStore( Iterable<Record> recordList, int mem ) {
		NaiveIndexStoreBuilder builder = new NaiveIndexStoreBuilder(recordList);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		tinvListAccessor = builder.buildTrInvList();
	}

	public IntList getInvList( int token ) {
		return invListAccessor.getList(token);
	}

	public IntList getTrInvList( int token ) {
		return tinvListAccessor.getList(token);
	}
}

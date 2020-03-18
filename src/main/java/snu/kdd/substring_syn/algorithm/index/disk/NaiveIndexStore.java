package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.record.Record;

public class NaiveIndexStore {

	final IndexStoreAccessor invListAccessor;
	final IndexStoreAccessor tinvListAccessor;
	
	public NaiveIndexStore( Iterable<Record> recordList ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE);
	}

	public NaiveIndexStore( Iterable<Record> recordList, int mem ) {
		NaiveIndexStoreBuilder builder = new NaiveIndexStoreBuilder(recordList);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		tinvListAccessor = builder.buildTrInvList();
	}

	public NaiveInvList getInvList( int token ) {
		int num = invListAccessor.getList(token);
		if ( invListAccessor.arr == null || num == 0 ) return null;
		else return new NaiveInvList(invListAccessor.arr, num);
	}

	public NaiveInvList getTrInvList( int token ) {
		int num = tinvListAccessor.getList(token);
		if ( tinvListAccessor.arr == null || num == 0 ) return null;
		else return new NaiveInvList(tinvListAccessor.arr, num);
	}
	
	public final BigInteger diskSpaceUsage() {
		return new BigInteger(""+(invListAccessor.size + tinvListAccessor.size));
	}
}

package snu.kdd.substring_syn.algorithm.index.disk;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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

	public IntList getInvList( int token ) {
		int[] arr = invListAccessor.getList(token);
		if ( arr == null ) return null;
		else return IntArrayList.wrap(arr);
	}

	public IntList getTrInvList( int token ) {
		int[] arr = tinvListAccessor.getList(token);
		if ( arr == null ) return null;
		else return IntArrayList.wrap(arr);
	}
	
	public final BigInteger diskSpaceUsage() {
		return new BigInteger(""+(invListAccessor.size + tinvListAccessor.size));
	}
}

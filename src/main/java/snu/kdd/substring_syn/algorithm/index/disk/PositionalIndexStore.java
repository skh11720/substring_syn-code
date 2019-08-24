package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public class PositionalIndexStore implements DiskBasedPositionalIndexInterface {

	final IndexStoreAccessor invListAccessor;
	final IndexStoreAccessor tinvListAccessor;
	
	public PositionalIndexStore( Iterable<Record> recordList ) {
		this(recordList, AbstractIndexStoreBuilder.INMEM_MAX_SIZE);
	}

	public PositionalIndexStore( Iterable<Record> recordList, int mem ) {
		PositionalIndexStoreBuilder builder = new PositionalIndexStoreBuilder(recordList);
		builder.setInMemMaxSize(mem);
		invListAccessor = builder.buildInvList();
		tinvListAccessor = builder.buildTrInvList();
	}

	public ObjectList<InvListEntry> getInvList( int token ) {
		int[] arr = invListAccessor.getList(token);
		if ( arr == null ) return null;
		else return getInvListFromArr(arr);
	}
	
	protected ObjectList<InvListEntry> getInvListFromArr( int[] arr ) {
		ObjectList<InvListEntry> invList = new ObjectArrayList<>();
		for ( int i=0; i<arr.length; i+=2 ) invList.add(new InvListEntry(arr[i], arr[i+1]));
		return invList;
	}

	public ObjectList<TransInvListEntry> getTrInvList( int token ) {
		int[] arr = tinvListAccessor.getList(token);
		if ( arr == null ) return null;
		else return getTrInvListFromArr(arr);
	}

	protected ObjectList<TransInvListEntry> getTrInvListFromArr( int[] arr ) {
		ObjectList<TransInvListEntry> invList = new ObjectArrayList<>();
		for ( int i=0; i<arr.length; i+=3 ) invList.add(new TransInvListEntry(arr[i], arr[i+1], arr[i+2]));
		return invList;
	}
}

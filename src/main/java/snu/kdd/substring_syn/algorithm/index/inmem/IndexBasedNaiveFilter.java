package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedNaiveInvertedIndex;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedNaiveFilter extends AbstractIndexBasedFilter {

	protected final DiskBasedNaiveInvertedIndex index;
	protected final boolean useCountFilter = true;
	
	public IndexBasedNaiveFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(dataset, theta, statContainer);
		index = new DiskBasedNaiveInvertedIndex(dataset.getIndexedList());
	}
	
	@Override
	public long invListSize() { return index.invListSize(); }
	
	@Override
	public long transInvListSize() { return index.transInvListSize(); }
	
	@Override
	public ObjectSet<Record> querySideFilter( Record query ) {
		IntSet candRidxSet = new IntOpenHashSet();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<Integer> invList = index.getInvList(token);
			if ( invList != null ) candRidxSet.addAll(invList);
		}
		ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
		candRidxSet.stream().forEach(id->candRecordSet.add(dataset.getRecord(id)));
		return candRecordSet;
	}
	
	@Override
	public ObjectSet<Record> textSideFilter( Record query ) {
		IntSet candRidxSet = new IntOpenHashSet();
		for ( int token : query.getTokens() ) {
			ObjectList<Integer> invList = index.getInvList(token);
			if ( invList != null ) candRidxSet.addAll(invList);
			ObjectList<Integer> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) candRidxSet.addAll(transInvList);
		}
		ObjectSet<Record> candRecordSet = new ObjectOpenHashSet<>();
		candRidxSet.stream().forEach(id->candRecordSet.add(dataset.getRecord(id)));
		return candRecordSet;
	}
}

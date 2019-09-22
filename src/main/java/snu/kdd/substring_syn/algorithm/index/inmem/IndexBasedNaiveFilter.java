package snu.kdd.substring_syn.algorithm.index.inmem;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
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
	public final int getNumInvFault() { return index.getNumInvFault(); }

	@Override
	public final int getNumTinvFault() { return index.getNumTinvFault(); }
	
	@Override
	public IntIterable querySideFilter( Record query ) {
		IntSet candRidxSet = new IntOpenHashSet();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<Integer> invList = index.getInvList(token);
			if ( invList != null ) candRidxSet.addAll(invList);
		}
		return candRidxSet;
	}
	
	@Override
	public IntIterable textSideFilter( Record query ) {
		IntSet candRidxSet = new IntOpenHashSet();
		for ( int token : query.getDistinctTokens() ) {
			ObjectList<Integer> invList = index.getInvList(token);
			if ( invList != null ) candRidxSet.addAll(invList);
			ObjectList<Integer> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) candRidxSet.addAll(transInvList);
		}
		return candRidxSet;
	}
}

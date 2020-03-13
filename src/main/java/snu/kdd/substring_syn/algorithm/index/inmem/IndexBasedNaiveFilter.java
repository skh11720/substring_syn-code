package snu.kdd.substring_syn.algorithm.index.inmem;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.algorithm.index.disk.DiskBasedNaiveInvertedIndex;
import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
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
		statContainer.startWatch(Stat.Time_QS_IndexFilter);
		IntSet candRidxSet = new IntOpenHashSet();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			NaiveInvList invList = index.getInvList(token);
			if ( invList != null ) {
				for ( int i=0; i<invList.size(); ++i ) candRidxSet.add(invList.getIdx(i));
			}
		}
		statContainer.stopWatch(Stat.Time_QS_IndexFilter);
		return candRidxSet;
	}
	
	@Override
	public IntIterable textSideFilter( Record query ) {
		statContainer.startWatch(Stat.Time_TS_IndexFilter);
		IntSet candRidxSet = new IntOpenHashSet();
		for ( int token : query.getDistinctTokens() ) {
			NaiveInvList invList = index.getInvList(token);
			if ( invList != null ) {
				for ( int i=0; i<invList.size(); ++i ) candRidxSet.add(invList.getIdx(i));
			}
			NaiveInvList transInvList = index.getTransInvList(token);
			if ( transInvList != null ) {
				for ( int i=0; i<transInvList.size(); ++i ) candRidxSet.add(transInvList.getIdx(i));
			}
		}
		statContainer.stopWatch(Stat.Time_TS_IndexFilter);
		return candRidxSet;
	}

	@Override
	public BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage();
	}
}

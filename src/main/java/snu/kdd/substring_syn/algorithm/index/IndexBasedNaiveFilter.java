package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedNaiveFilter extends AbstractIndexBasedFilter {

	protected final NaiveInvertedIndex index;
	protected final boolean useCountFilter = true;
	
	public IndexBasedNaiveFilter( Dataset dataset, double theta, StatContainer statContainer ) {
		super(theta, statContainer);
		index = new NaiveInvertedIndex(dataset);
	}
	
	@Override
	public ObjectSet<RecordInterface> querySideFilter( Record query ) {
		ObjectSet<RecordInterface> candRecordSet = new ObjectOpenHashSet<>();
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<Record> invList = index.getInvList(token);
			if ( invList != null ) candRecordSet.addAll(invList);
		}
		return candRecordSet;
	}
	
	@Override
	public ObjectSet<RecordInterface> textSideFilter( Record query ) {
		ObjectSet<RecordInterface> candRecordSet = new ObjectOpenHashSet<>();
		for ( int token : query.getTokens() ) {
			ObjectList<Record> invList = index.getInvList(token);
			if ( invList != null ) candRecordSet.addAll(invList);
			ObjectList<Record> transInvList = index.getTransInvList(token);
			if ( transInvList != null ) candRecordSet.addAll(transInvList);
		}
		return candRecordSet;
	}
}

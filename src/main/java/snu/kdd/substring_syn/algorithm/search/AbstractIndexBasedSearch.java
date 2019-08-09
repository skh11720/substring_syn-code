package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.index.AbstractIndexBasedFilter;
import snu.kdd.substring_syn.algorithm.index.IndexBasedCountFilter;
import snu.kdd.substring_syn.algorithm.index.IndexBasedNaiveFilter;
import snu.kdd.substring_syn.algorithm.index.IndexBasedPositionFilter;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public abstract class AbstractIndexBasedSearch extends AbstractSearch {
	
	public static enum IndexChoice {
		None,
		Naive,
		Count,
		Position,
	}

	protected AbstractIndexBasedFilter indexFilter;
	protected final IndexChoice indexChoice;


	public AbstractIndexBasedSearch( double theta, IndexChoice indexChoice ) {
		super(theta);
		this.indexChoice = indexChoice; 
	}

	@Override
	protected void prepareSearch( Dataset dataset ) {
		buildIndex(dataset);
	}
	
	protected void buildIndex( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_BuildIndex);
		double mem_before = Util.getMemoryUsage();
		indexFilter = buildSpecificIndex(dataset);
		double mem_after = Util.getMemoryUsage();
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.addCount(Stat.Size_Index_InvList, indexFilter.invListSize());
		statContainer.addCount(Stat.Size_Index_TransInvList, indexFilter.transInvListSize());
		statContainer.setStat(Stat.Mem_Before_Index, String.format("%.3f", mem_before));
		statContainer.setStat(Stat.Mem_After_Index, String.format("%.3f", mem_after));
		Log.log.info("[MEM] before building index: %.3f MB", mem_before);
		Log.log.info("[MEM] after building index: %.3f MB", mem_after);
	}

	protected AbstractIndexBasedFilter buildSpecificIndex(Dataset dataset) {
		switch(indexChoice) {
		case None: return null;
		case Naive: return new IndexBasedNaiveFilter(dataset, theta, statContainer);
		case Count: return new IndexBasedCountFilter(dataset, theta, statContainer);
		case Position: return new IndexBasedPositionFilter(dataset, theta, statContainer);
		default: throw new RuntimeException("Unknown index type: "+indexChoice);
		}
	}

	@Override
	protected void searchQuerySide( Record query, Dataset dataset ) {
		Iterable<? extends RecordInterface> candListQuerySide = getCandRecordListQuerySide(query, dataset);
		for ( RecordInterface rec : candListQuerySide ) {
			statContainer.addCount(Stat.Len_QS_Retrieved, rec.size());
			searchRecordQuerySide(query, rec);
		}
	}
	
	@Override
	protected void searchTextSide( Record query, Dataset dataset ) {
		Iterable<? extends RecordInterface> candListTextSide = getCandRecordListTextSide(query, dataset);
		for ( RecordInterface rec : candListTextSide ) {
			statContainer.addCount(Stat.Len_TS_Retrieved, rec.size());
			searchRecordTextSide(query, rec);
		}
	}
	
	protected Iterable<? extends RecordInterface> getCandRecordListQuerySide( Record query, Dataset dataset ) {
		if ( indexFilter != null ) {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			ObjectSet<RecordInterface> candRecordSet = indexFilter.querySideFilter(query);
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
			return candRecordSet;
		}
		else return dataset.indexedList;
	}
	
	protected Iterable<? extends RecordInterface> getCandRecordListTextSide( Record query, Dataset dataset ) {
		if ( indexFilter != null ) {
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			ObjectSet<RecordInterface> candRecordSet = indexFilter.textSideFilter(query);
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
			return candRecordSet;
		}
		else return dataset.indexedList;
	}
}

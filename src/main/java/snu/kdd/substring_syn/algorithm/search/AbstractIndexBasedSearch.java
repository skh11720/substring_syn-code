package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.index.inmem.AbstractIndexBasedFilter;
import snu.kdd.substring_syn.algorithm.index.inmem.IndexBasedCountFilter;
import snu.kdd.substring_syn.algorithm.index.inmem.IndexBasedNaiveFilter;
import snu.kdd.substring_syn.algorithm.index.inmem.IndexBasedPositionFilter;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public abstract class AbstractIndexBasedSearch extends AbstractSearch {
	
	public static enum IndexChoice {
		None,
		Naive,
		Count,
		CountPosition,
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
		if ( indexFilter != null ) {
			statContainer.addCount(Stat.Size_Index_InvList, indexFilter.invListSize());
			statContainer.addCount(Stat.Size_Index_TransInvList, indexFilter.transInvListSize());
		}
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
		case CountPosition: return new IndexBasedPositionFilter(dataset, theta, true, statContainer);
		case Position: return new IndexBasedPositionFilter(dataset, theta, false, statContainer);
		default: throw new RuntimeException("Unknown index type: "+indexChoice);
		}
	}

	@Override
	protected Iterable<Record> getCandRecordListQuerySide( Record query, Dataset dataset ) {
		if ( indexFilter != null ) {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			Iterable<Record> candRecordSet = indexFilter.getCandRecordsQuerySide(query);
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
			return candRecordSet;
		}
		else return dataset.getIndexedList();
	}
	
	@Override
	protected Iterable<Record> getCandRecordListTextSide( Record query, Dataset dataset ) {
		if ( indexFilter != null ) {
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			Iterable<Record> candRecordSet = indexFilter.getCandRecordsTextSide(query);
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
			return candRecordSet;
		}
		else return dataset.getIndexedList();
	}
}

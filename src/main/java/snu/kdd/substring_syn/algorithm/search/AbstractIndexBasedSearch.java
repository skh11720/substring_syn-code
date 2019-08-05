package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.index.AbstractIndexBasedFilter;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Stat;

public abstract class AbstractIndexBasedSearch extends AbstractSearch {
	
	protected AbstractIndexBasedFilter indexFilter;
	protected final boolean bIF;

	public AbstractIndexBasedSearch( double theta, boolean bIF ) {
		super(theta);
		this.bIF = bIF;
		param.put("bIF", Boolean.toString(bIF));
	}

	@Override
	protected void prepareSearch( Dataset dataset ) {
		if (bIF) buildIndex(dataset);
	}
	
	protected void buildIndex( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_BuildIndex);
		indexFilter = buildSpecificIndex(dataset);
		statContainer.stopWatch(Stat.Time_BuildIndex);
	}
	
	protected abstract AbstractIndexBasedFilter buildSpecificIndex( Dataset dataset );

	@Override
	protected void searchQuerySide( Record query, Dataset dataset ) {
		Iterable<? extends RecordInterface> candListQuerySide = getCandRecordListQuerySide(query, dataset);
		for ( RecordInterface rec : candListQuerySide ) {
			searchRecordQuerySide(query, rec);
		}
	}
	
	@Override
	protected void searchTextSide( Record query, Dataset dataset ) {
		Iterable<? extends RecordInterface> candListTextSide = getCandRecordListTextSide(query, dataset);
		for ( RecordInterface rec : candListTextSide ) {
			searchRecordTextSide(query, rec);
		}
	}
	
	protected Iterable<? extends RecordInterface> getCandRecordListQuerySide( Record query, Dataset dataset ) {
		if (bIF) {
			statContainer.startWatch(Stat.Time_QS_IndexFilter);
			ObjectSet<RecordInterface> candRecordSet = indexFilter.querySideFilter(query);
			statContainer.stopWatch(Stat.Time_QS_IndexFilter);
			return candRecordSet;
		}
		else return dataset.indexedList;
	}
	
	protected Iterable<? extends RecordInterface> getCandRecordListTextSide( Record query, Dataset dataset ) {
		if (bIF) {
			statContainer.startWatch(Stat.Time_TS_IndexFilter);
			ObjectSet<RecordInterface> candRecordSet = indexFilter.textSideFilter(query);
			statContainer.stopWatch(Stat.Time_TS_IndexFilter);
			return candRecordSet;
		}
		else return dataset.indexedList;
	}
}

package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.index.NaiveIndexBasedFilter;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.utils.Stat;

public abstract class AbstractIndexBasedSearch extends AbstractSearch {
	
	protected NaiveIndexBasedFilter indexFilter;
	protected final boolean idxFilter_query;
	protected final boolean idxFilter_text;

	public AbstractIndexBasedSearch( double theta, boolean idxFilter_query, boolean idxFilter_text ) {
		super(theta);
		this.idxFilter_query = idxFilter_query;
		this.idxFilter_text = idxFilter_text;
		param.put("idxFilter_query", Boolean.toString(idxFilter_query));
		param.put("idxFilter_text", Boolean.toString(idxFilter_text));
	}

	@Override
	protected void prepareSearch( Dataset dataset ) {
		if (idxFilter_query || idxFilter_text) buildIndex(dataset);
	}
	
	protected void buildIndex( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_4_BuildIndex);
		indexFilter = new NaiveIndexBasedFilter(dataset, theta, statContainer);
		statContainer.stopWatch(Stat.Time_4_BuildIndex);
	}

	@Override
	protected void searchGivenQuery( Record query, Dataset dataset ) {
		Iterable<? extends RecordInterface> candListQuerySide = getCandRecordListQuerySide(query, dataset);
		searchQuerySide(query, candListQuerySide);
		Iterable<Record> candListTextSide = getCandRecordListTextSide(query, dataset);
		searchTextSide(query, candListTextSide);
	}
	
	protected Iterable<? extends RecordInterface> getCandRecordListQuerySide( Record query, Dataset dataset ) {
		if (idxFilter_query) {
			statContainer.startWatch(Stat.Time_5_IndexFilter);
			ObjectSet<RecordInterface> candRecordSet = indexFilter.querySideFilter(query);
			statContainer.stopWatch(Stat.Time_5_IndexFilter);
			return candRecordSet;
		}
		else return dataset.indexedList;
	}
	
	protected Iterable<Record> getCandRecordListTextSide( Record query, Dataset dataset ) {
		if (idxFilter_text) {
			statContainer.startWatch(Stat.Time_5_IndexFilter);
			ObjectSet<Record> candRecordSet = indexFilter.textSideFilter(query);
			statContainer.stopWatch(Stat.Time_5_IndexFilter);
			return candRecordSet;
		}
		else return dataset.indexedList;
	}
}

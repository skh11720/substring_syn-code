package snu.kdd.substring_syn.algorithm.search;

import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.IndexBasedFilter;
import snu.kdd.substring_syn.algorithm.index.InvertedIndex;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Stat;

public abstract class AbstractIndexBasedSearch extends AbstractSearch {
	
	protected InvertedIndex index;
	protected IndexBasedFilter indexFilter;
	protected final boolean idxFilter_query;

	public AbstractIndexBasedSearch( double theta, boolean idxFilter_query ) {
		super(theta);
		this.idxFilter_query = idxFilter_query;
		param.put("idxFilter_query", Boolean.toString(idxFilter_query));
	}

	@Override
	protected void prepareSearch( Dataset dataset ) {
		if (idxFilter_query) buildIndex(dataset);
	}
	
	protected void buildIndex( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_4_BuildIndex);
		index = new InvertedIndex(dataset);
		indexFilter = new IndexBasedFilter(index, theta);
		statContainer.stopWatch(Stat.Time_4_BuildIndex);
	}
	
	@Override
	protected void searchQuery( Record query, Dataset dataset ) {
		if (idxFilter_query) {
			statContainer.startWatch(Stat.Time_5_IndexFilter);
			ObjectList<Record> candRecordList = indexFilter.querySideCountFilter(query);
			statContainer.stopWatch(Stat.Time_5_IndexFilter);
			statContainer.addCount(Stat.Num_QS_IndexFiltered, dataset.indexedList.size() - candRecordList.size());
			search(query, candRecordList);
		}
		else search(query, dataset.indexedList);
	}
}

package snu.kdd.substring_syn.algorithm.index.inmem;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class IndexBasedCountFilter extends IndexBasedNaiveFilter {

	public IndexBasedCountFilter(Dataset dataset, double theta, StatContainer statContainer) {
		super(dataset, theta, statContainer);
	}
	
	@Override
	protected int getQuerySideMinCount(Record query) {
		return (int)Math.ceil(theta*query.getMinTransLength());
	}
	
	@Override
	protected int getTextSideMinCount(Record query) {
		return (int)Math.ceil(theta*query.size());
	}
}

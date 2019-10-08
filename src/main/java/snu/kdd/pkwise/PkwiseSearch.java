package snu.kdd.pkwise;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;

public class PkwiseSearch extends AbstractSearch {
	
	protected final int qlen;
	protected final int kmax;

	public PkwiseSearch( double theta, int qlen, int kmax ) {
		super(theta);
		this.qlen = qlen;
		this.kmax = kmax;
		param.put("qlen", Integer.toString(qlen));
		param.put("kmax", Integer.toString(kmax));
	}

	@Override
	protected Iterable<Record> getCandRecordListQuerySide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}

	@Override
	protected Iterable<Record> getCandRecordListTextSide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}

	@Override
	protected void searchRecordQuerySide(Record query, Record rec) {
	}

	@Override
	protected void searchRecordTextSide(Record query, Record rec) {
	}

	@Override
	public String getName() {
		return "PkwiseSearch";
	}

	@Override
	public String getVersion() {
		return "0.00";
	}

}

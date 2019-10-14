package snu.kdd.pkwise;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseSearch extends PkwiseNaiveSearch {
	
	private PkwiseIndex index;

	public PkwiseSearch( double theta, int qlen, int kmax ) {
		super(theta, qlen, kmax);
	}

	@Override
	protected void prepareSearch(Dataset dataset) {
		super.prepareSearch(dataset);
        index = new PkwiseIndex(this, ((WindowDataset)dataset), qlen, theta);
        index.writeToFile();
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
	}
	
	@Override
	protected Iterable<Subrecord> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
		return index.getCandWindowQuerySide(query);
	}
	
	@Override
	public String getName() {
		return "PkwiseSearch";
	}

	@Override
	public String getVersion() {
		return "0.01";
	}
}

package snu.kdd.pkwise;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;

public class PkwiseSearch extends PkwiseNaiveSearch {
	
	private PkwiseIndex index;
	protected final TokenPartitioner partitioner;
	protected final KwiseSignatureMap sigMap;
	protected final PkwiseSignatureGenerator siggen;

	public PkwiseSearch( double theta, int qlen, int kmax ) {
		super(theta, qlen, kmax);
		partitioner = new TokenPartitioner(kmax);
		sigMap = new KwiseSignatureMap();
		siggen = new PkwiseSignatureGenerator(partitioner, sigMap, kmax);
	}
	
	public final PkwiseSignatureGenerator getSiggen() { return siggen; }

	@Override
	protected void prepareSearch(Dataset dataset) {
		super.prepareSearch(dataset);
        index = new PkwiseIndex(this, ((WindowDataset)dataset), qlen, theta);
//        index.writeToFile(sigMap);
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
	}
	
	@Override
	protected Iterable<RecordInterface> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
		return index.getCandWindowQuerySide(query, siggen);
	}
	
	@Override
	public String getName() {
		return "PkwiseSearch";
	}

	@Override
	public String getVersion() {
		return "0.03";
	}
}

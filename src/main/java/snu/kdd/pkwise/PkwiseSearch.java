package snu.kdd.pkwise;


import org.apache.logging.log4j.Level;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.WindowDataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class PkwiseSearch extends PkwiseNaiveSearch {
	
	private PkwiseIndex index;
	protected String kmax;
	protected TokenPartitioner partitioner;
	protected KwiseSignatureMap sigMap;
	protected PkwiseSignatureGenerator siggen;

	public PkwiseSearch( double theta, int qlen, String kmax ) {
		super(theta, qlen);
		this.kmax = kmax;
		param.put("kmax", kmax);
	}
	
	
	public final PkwiseSignatureGenerator getSiggen() { return siggen; }

	public final String tok2str(int token) {
		if ( token <= Record.tokenIndex.getMaxID() ) return Record.tokenIndex.getToken(token);
		else return sigMap.get(token).toOriginalString();
	}

	@Override
	protected void prepareSearch(Dataset dataset) {
		super.prepareSearch(dataset);
		int kmax = getKMaxValue();
		partitioner = new TokenPartitioner(kmax);
		sigMap = new KwiseSignatureMap();
		siggen = new PkwiseSignatureGenerator(partitioner, sigMap, kmax);
		buildIndex();
	}
	
	protected int getKMaxValue() {
		try {
			return Integer.parseInt(kmax);
		}
		catch(NumberFormatException e) {
			return 0;
		}
	}
	
	protected void buildIndex() {
		statContainer.startWatch(Stat.Time_BuildIndex);
        index = new PkwiseIndex(this, ((WindowDataset)dataset), qlen, theta);
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.setStat(Stat.Space_Index, index.diskSpaceUsage().toString());
		if ( Log.log.getLevel().isLessSpecificThan(Level.INFO)) index.writeToFile(sigMap);
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
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}
}

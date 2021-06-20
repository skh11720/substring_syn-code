package snu.kdd.pkwise;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.WindowDataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseNaiveSearch extends AbstractSearch {
	
	protected final int qlen;

	public PkwiseNaiveSearch( double theta, int qlen ) {
		super(theta);
		this.qlen = qlen;
		param.put("qlen", Integer.toString(qlen));
	}
	
	@Override
	protected void prepareSearchGivenQuery(Record query) {
	}
	
	@Override
	protected void searchBody(Dataset dataset) {
		pkwiseSearch((WindowDataset)dataset);
	}
	
	protected final void pkwiseSearch( WindowDataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			long ts = System.nanoTime();
			pkwiseSearchGivenQuery(query, dataset);
			double searchTime = (System.nanoTime()-ts)/1e6;
			statContainer.addSampleValue(Stat.Time_SearchPerQuery, searchTime);
			Log.log.info("search(query=%d, ...)\t%.3f ms", ()->query.getIdx(), ()->searchTime);
		}
	}

	protected void pkwiseSearchGivenQuery( Record query, WindowDataset dataset ) {
		prepareSearchGivenQuery(query);
		statContainer.startWatch(Stat.Time_QS_Total);
		pkwiseSearchQuerySide(query, dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
	}

	protected void pkwiseSearchQuerySide( Record query, WindowDataset dataset ) {
		Iterable<RecordInterface> candListQuerySide = getCandWindowListQuerySide(query, dataset);
		for ( RecordInterface window : candListQuerySide ) {
			if (rsltQuerySideContains(query, window)) continue;
			statContainer.addCount(Stat.Len_QS_Retrieved, window.size());
			searchWindowQuerySide(query, window);
		}
	}
	
	protected Iterable<RecordInterface> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
		return Util.convert(dataset.getWindowList(qlen, qlen));
	}
	
	protected final void searchWindowQuerySide(Record query, RecordInterface window) {
		statContainer.startWatch(Stat.Time_QS_Validation);
		double sim = verifyQuerySide(query, window);
		statContainer.stopWatch(Stat.Time_QS_Validation);
		if ( sim >= theta ) {
			addResultQuerySide(query, window);
			return;
		}
	}
	
	protected double verifyQuerySide( Record query, RecordInterface window ) {
		statContainer.addCount(Stat.Num_QS_Verified, 1);
		return Util.jaccardM(query.getTokenList(), window.getTokenList());
	}
	
	@Override
	protected Iterable<TransformableRecordInterface> getCandRecordListQuerySide(Record query, Dataset dataset) { return null; }
	@Override
	protected Iterable<TransformableRecordInterface> getCandRecordListTextSide(Record query, Dataset dataset) { return null; }
	@Override
	protected void searchRecordQuerySide(Record query, RecordInterface rec) { } 
	@Override
	protected void searchRecordTextSide(Record query, TransformableRecordInterface rec) { } 

	@Override
	public String getName() {
		return "PkwiseNaiveSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

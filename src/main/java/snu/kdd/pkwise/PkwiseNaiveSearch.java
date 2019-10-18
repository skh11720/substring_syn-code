package snu.kdd.pkwise;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseNaiveSearch extends AbstractSearch {
	
	protected final int qlen;
	protected final int kmax;

	public PkwiseNaiveSearch( double theta, int qlen, int kmax ) {
		super(theta);
		this.qlen = qlen;
		this.kmax = kmax;
		param.put("qlen", Integer.toString(qlen));
		param.put("kmax", Integer.toString(kmax));
	}

	@Override
	public final void run( Dataset dataset ) {
		statContainer.setAlgorithm(this);
		statContainer.mergeStatContainer(dataset.statContainer);
		statContainer.startWatch(Stat.Time_Total);
		prepareSearch(dataset);
		pkwiseSearch((WindowDataset)dataset);
		statContainer.stopWatch(Stat.Time_Total);
		putResultIntoStat();
		statContainer.finalizeAndOutput();
		outputResult(dataset);
	}
	
	@Override
	protected void prepareSearchGivenQuery(Record query) {
	}
	
	protected final void pkwiseSearch( WindowDataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			long ts = System.nanoTime();
			pkwiseSearchGivenQuery(query, dataset);
			double searchTime = (System.nanoTime()-ts)/1e6;
			statContainer.addSampleValue("Time_SearchPerQuery", searchTime);
			Log.log.info("search(query=%d, ...)\t%.3f ms", ()->query.getID(), ()->searchTime);
		}
	}

	protected void pkwiseSearchGivenQuery( Record query, WindowDataset dataset ) {
//		if ( query.getID() != 0 ) return;
		prepareSearchGivenQuery(query);
		statContainer.startWatch(Stat.Time_QS_Total);
		pkwiseSearchQuerySide(query, dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
	}

	protected void pkwiseSearchQuerySide( Record query, WindowDataset dataset ) {
		Iterable<RecordInterface> candListQuerySide = getCandWindowListQuerySide(query, dataset);
		for ( RecordInterface window : candListQuerySide ) {
			if ( rsltQuerySide.contains(new IntPair(query.getID(), window.getID())) ) continue;
//			if ( window.getID() != 7324 ) continue;
			statContainer.addCount(Stat.Len_QS_Retrieved, window.size());
			searchWindowQuerySide(query, window);
		}
	}
	
	protected Iterable<RecordInterface> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
		return dataset.getWindowList(qlen, qlen);
	}
	
	protected final void searchWindowQuerySide(Record query, RecordInterface window) {
		statContainer.startWatch(Stat.Time_QS_Validation);
		double sim = verifyQuerySide(query, window);
		statContainer.stopWatch(Stat.Time_QS_Validation);
		Log.log.trace("q=[%d]  %s", query.getID(), query.toOriginalString());
		Log.log.trace("w=[%d]  %s", window.getID(), window.toOriginalString());
		Log.log.trace("sim=%.3f", sim);
		if ( sim >= theta ) {
			rsltQuerySide.add(new IntPair(query.getID(), window.getID()));
			Log.log.trace("rsltQuerySide = %d", rsltQuerySide.size());
			return;
		}
	}
	
	protected double verifyQuerySide( Record query, RecordInterface window ) {
		statContainer.addCount(Stat.Num_QS_Verified, 1);
		return Util.jaccardM(query.getTokenList(), window.getTokenList());
	}
	
	@Override
	protected Iterable<Record> getCandRecordListQuerySide(Record query, Dataset dataset) { return null; }
	@Override
	protected Iterable<Record> getCandRecordListTextSide(Record query, Dataset dataset) { return null; }
	@Override
	protected void searchRecordQuerySide(Record query, Record rec) { } 
	@Override
	protected void searchRecordTextSide(Record query, Record rec) { } 

	@Override
	public String getName() {
		return "PkwiseNaiveSearch";
	}

	@Override
	public String getVersion() {
		return "0.02";
	}
}

package snu.kdd.pkwise;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseSynSearch extends PkwiseSearch {

	private PkwiseSynIndex index;
	
	public PkwiseSynSearch( double theta, int qlen, int kmax ) {
		super(theta, qlen, kmax);
	}

	@Override
	protected void prepareSearch(Dataset dataset) {
		statContainer.startWatch(Stat.Time_BuildIndex);
        index = new PkwiseSynIndex(this, ((TransWindowDataset)dataset), qlen, theta);
		statContainer.stopWatch(Stat.Time_BuildIndex);
        index.writeToFile(sigMap);
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
		query.preprocessAll();
	}
	
	@Override
	protected void pkwiseSearchGivenQuery( Record query, WindowDataset dataset ) {
//		if ( query.getID() != 16 ) return;
		statContainer.startWatch(Stat.Time_Preprocess);
		prepareSearchGivenQuery(query);
		statContainer.stopWatch(Stat.Time_Preprocess);
		statContainer.startWatch(Stat.Time_QS_Total);
		for ( Record queryExp : Records.expands(query) ) {
			Log.log.trace("queryExp=%s\t%s", queryExp, queryExp.toOriginalString());
			pkwiseSearchQuerySide(queryExp, dataset);
		}
		statContainer.stopWatch(Stat.Time_QS_Total);
		statContainer.startWatch(Stat.Time_TS_Total);
		pkwiseSearchTextSide(query, dataset);
		statContainer.stopWatch(Stat.Time_TS_Total);
	}

	protected void pkwiseSearchQuerySide( Record query, WindowDataset dataset ) {
		Iterable<RecordInterface> candListQuerySide = getCandWindowListQuerySide(query, dataset);
		for ( RecordInterface window : candListQuerySide ) {
			if ( rsltQuerySide.contains(new IntPair(query.getID(), window.getID())) ) continue;
//			if ( window.getID() >= 0 ) continue;
			statContainer.addCount(Stat.Len_QS_Retrieved, window.size());
			searchWindowQuerySide(query, window);
		}
	}

	protected final void pkwiseSearchTextSide( Record query, WindowDataset dataset ) {
		Iterable<RecordInterface> candListTextSide = getCandWindowListTextSide(query, dataset);
		for ( RecordInterface window : candListTextSide ) {
			if ( rsltTextSide.contains(new IntPair(query.getID(), window.getID())) ) continue;
//			if ( window.getID() != 6116 ) continue;
			statContainer.addCount(Stat.Len_TS_Retrieved, window.size());
			statContainer.startWatch("Time_TS_searchTextSide.preprocess");
			window.getSuperRecord().preprocessAll();
			statContainer.stopWatch("Time_TS_searchTextSide.preprocess");
			searchWindowTextSide(query, window);
		}
	}
	
	@Override
	protected Iterable<RecordInterface> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
//		return dataset.getWindowList(wMin, wMax);
		return index.getCandWindowQuerySide(query);
	}
	
	protected Iterable<RecordInterface> getCandWindowListTextSide(Record query, WindowDataset dataset ) {
//		return dataset.getTransWindowList(qlen, theta);
		return index.getCandWindowTextSide(query);
	}

	protected final void searchWindowTextSide(Record query, RecordInterface window) {
//		Log.log.trace("searchWindowTextSide: query=%s\t%s", query, query.toOriginalString());
//		Log.log.trace("searchWindowTextSide: window=%s\t%s", window, window.toOriginalString());
		statContainer.startWatch(Stat.Time_TS_Validation);
		statContainer.addCount(Stat.Num_TS_Verified, 1);
		double sim = Util.jaccardM(query.getTokenList(), window.getTokenList());
		statContainer.stopWatch(Stat.Time_TS_Validation);
		if ( sim >= theta ) {
			rsltTextSide.add(new IntPair(query.getID(), window.getID()));
			return;
		}
	}
	
	@Override
	protected Iterable<Record> getCandRecordListQuerySide(Record query, Dataset dataset) { return null; }
	@Override
	protected Iterable<Record> getCandRecordListTextSide(Record query, Dataset dataset) { return null; }
	@Override
	protected void searchRecordQuerySide(Record query, Record rec) { } 
	@Override
	protected void searchRecordTextSide(Record query, Record rec) { } 

	public final int getLFLB( int size ) {
		return (int)Math.ceil(1.0*size*theta);
	}
	
	public final int getLFUB( int size ) {
		return (int)(1.0*size/theta);
	}
	
	@Override
	public final String getName() {
		return "PkwiseSynSearch";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: modify IntQGramStore
		 */
		return "1.02";
	}
}

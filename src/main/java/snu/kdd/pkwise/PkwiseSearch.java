package snu.kdd.pkwise;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class PkwiseSearch extends AbstractSearch {
	
	protected final int qlen;
	protected final int kmax;
	protected final GreedyValidator validator;

	public PkwiseSearch( double theta, int qlen, int kmax ) {
		super(theta);
		this.qlen = qlen;
		this.kmax = kmax;
		param.put("qlen", Integer.toString(qlen));
		param.put("kmax", Integer.toString(kmax));
		validator = new GreedyValidator(theta, statContainer);
	}

	public void run( Dataset dataset ) {
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
	protected void prepareSearch(Dataset dataset) {
		super.prepareSearch(dataset);
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
		System.out.println("preprocess query "+query.getID());
		query.preprocessAll();
		System.out.println(query);
		for ( int k=0; k<query.size(); ++k ) {
			int n = 0;
			for ( Rule rule : query.getApplicableRules(k) ) ++n;
			System.out.println("nAppRules: "+ k+"\t"+n);
		}
		System.out.println(query.toStringDetails());
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

	protected final void pkwiseSearchGivenQuery( Record query, WindowDataset dataset ) {
//		if ( query.getID() != 0 ) return;
		prepareSearchGivenQuery(query);
		statContainer.startWatch(Stat.Time_QS_Total);
		pkwiseSearchQuerySide(query, dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
		statContainer.startWatch(Stat.Time_TS_Total);
		pkwiseSearchTextSide(query, dataset);
		statContainer.stopWatch(Stat.Time_TS_Total);
	}

	protected final void pkwiseSearchQuerySide( Record query, WindowDataset dataset ) {
		Iterable<Subrecord> candListQuerySide = getCandWindowListQuerySide(query, dataset);
		for ( Subrecord window : candListQuerySide ) {
			if ( rsltQuerySide.contains(new IntPair(query.getID(), window.getID())) ) continue;
//			if ( window.getID() != 7677 ) continue;
			statContainer.addCount(Stat.Len_QS_Retrieved, window.size());
			searchWindowQuerySide(query, window);
		}
	}
	
	protected final void pkwiseSearchTextSide( Record query, WindowDataset dataset ) {
		Iterable<Subrecord> candListTextSide = getCandWindowListTextSide(query, dataset);
		for ( Subrecord window : candListTextSide ) {
			if ( rsltTextSide.contains(new IntPair(query.getID(), window.getID())) ) continue;
//			if ( window.getID() != 946 ) continue;
			statContainer.addCount(Stat.Len_TS_Retrieved, window.size());
			statContainer.startWatch("Time_TS_searchTextSide.preprocess");
			window.getSuperRecord().preprocessAll();
			statContainer.stopWatch("Time_TS_searchTextSide.preprocess");
			searchWindowTextSide(query, window);
		}
	}
	
	protected final Iterable<Subrecord> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
		int wMin = getLFLB(qlen);
		int wMax = getLFUB(qlen);
		return dataset.getWindowList(wMin, wMax);
	}
	
	protected final Iterable<Subrecord> getCandWindowListTextSide(Record query, WindowDataset dataset ) {
		return dataset.getTransWindowList(qlen, theta);
	}
	
	protected final void searchWindowQuerySide(Record query, Subrecord window) {
		statContainer.startWatch(Stat.Time_QS_Validation);
		double sim = validator.simQuerySide(query, window);
		statContainer.stopWatch(Stat.Time_QS_Validation);
//		Log.log.trace("q=[%d]  %s", query.getID(), query.toOriginalString());
//		Log.log.trace("w=[%d]  %s", window.getID(), window.toOriginalString());
//		Log.log.trace("sim=%.3f", sim);
		if ( sim >= theta ) {
			rsltQuerySide.add(new IntPair(query.getID(), window.getID()));
//			Log.log.trace("rsltQuerySide = %d", rsltQuerySide.size());
			return;
		}
	}
	
	protected final void searchWindowTextSide(Record query, Subrecord window) {
//		Log.log.trace("searchWindowTextSide: query=%d, window=[%d,%d,%d]", query.getID(), window.getID(), window.sidx, window.eidx);
		statContainer.startWatch(Stat.Time_TS_Validation);
		double sim = validator.simTextSide(query, window);
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
		return "PkwiseSearch";
	}

	@Override
	public String getVersion() {
		return "0.01";
	}

}

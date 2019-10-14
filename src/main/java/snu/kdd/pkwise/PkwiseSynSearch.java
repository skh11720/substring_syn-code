package snu.kdd.pkwise;

import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;

public class PkwiseSynSearch extends PkwiseSearch {

	private GreedyValidator validator;
	private PkwiseSynIndex index;
	
	public PkwiseSynSearch( double theta, int qlen, int kmax ) {
		super(theta, qlen, kmax);
		validator = new GreedyValidator(theta, statContainer);
	}

	@Override
	protected void prepareSearch(Dataset dataset) {
		super.prepareSearch(dataset);
        index = new PkwiseSynIndex(this, ((WindowDataset)dataset), qlen, theta);
        index.writeToFile();
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
		query.preprocessAll();
	}
	
	@Override
	protected void pkwiseSearchGivenQuery( Record query, WindowDataset dataset ) {
//		if ( query.getID() != 0 ) return;
		prepareSearchGivenQuery(query);
		statContainer.startWatch(Stat.Time_QS_Total);
		pkwiseSearchQuerySide(query, dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
		statContainer.startWatch(Stat.Time_TS_Total);
		pkwiseSearchTextSide(query, dataset);
		statContainer.stopWatch(Stat.Time_TS_Total);
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
	
	@Override
	protected Iterable<Subrecord> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
//		return dataset.getWindowList(wMin, wMax);
		return index.getCandWindowQuerySide(query);
	}
	
	protected Iterable<Subrecord> getCandWindowListTextSide(Record query, WindowDataset dataset ) {
//		return dataset.getTransWindowList(qlen, theta);
		return index.getCandWindowTextSide(query);
	}

	@Override
	protected double verifyQuerySide( Record query, Subrecord window ) {
		return validator.simQuerySide(query, window);
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
		return "PkwiseSynSearch";
	}

	@Override
	public String getVersion() {
		return "0.05";
	}
}

package snu.kdd.pkwise;

import java.math.BigInteger;

import org.apache.logging.log4j.Level;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TransWindowDataset;
import snu.kdd.substring_syn.data.WindowDataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseSynSearch extends PkwiseSearch {

	private static final int KMAX = 2;
	private PkwiseSynIndex index;



	public static int getMinTransQueryLen( Dataset dataset ) {
		int len = Integer.MAX_VALUE;
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			len = Math.min(len, query.getMinTransLength());
		}
		return len;
	}
	
	public static int computeMaxK( int minQueryTransLen, double theta ) {
		return Math.min(KMAX, (int)(Math.sqrt(2*(Math.ceil(theta*minQueryTransLen)-1)+0.25)+0.5));
	}


	
	public PkwiseSynSearch( double theta, int qlen, String kmax ) {
		super(theta, qlen, kmax);
	}

	@Override
	protected int getKMaxValue() {
		if ( kmax.equals("opt") ) {
			int minQueryTransLen = getMinTransQueryLen(dataset);
			int kmax = computeMaxK(minQueryTransLen, theta);
			statContainer.setStat("Chosen_Kmax", ""+kmax);
			return kmax;
		}
		else {
			try {
				return Integer.parseInt(kmax);
			}
			catch(NumberFormatException e) {
				return 0;
			}
		}
	}
	
	@Override
	protected void buildIndex() {
		statContainer.startWatch(Stat.Time_BuildIndex);
        index = new PkwiseSynIndex(this, ((TransWindowDataset)dataset), qlen, theta);
		statContainer.stopWatch(Stat.Time_BuildIndex);
		statContainer.setStat(Stat.Space_Index, index.diskSpaceUsage().toString());
		if ( Log.log.getLevel().isLessSpecificThan(Level.INFO)) index.writeToFile(sigMap);
	}

	@Override
	protected void prepareSearchGivenQuery(Record query) {
		query.preprocessAll();
	}
	
	@Override
	protected void pkwiseSearchGivenQuery( Record query, WindowDataset dataset ) {
//		if ( query.getID() != 66 ) return;
		statContainer.startWatch(Stat.Time_Preprocess);
		prepareSearchGivenQuery(query);
		statContainer.stopWatch(Stat.Time_Preprocess);
		statContainer.startWatch(Stat.Time_QS_Total);
		for ( Record queryExp : Records.expands(query) ) {
//			Log.log.trace("queryExp=%s\t%s", ()->queryExp, ()->queryExp.toOriginalString());
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
			if (rsltQuerySideContains(query, window)) continue;
//			if ( window.getID() >= 0 ) continue;
			statContainer.addCount(Stat.Len_QS_Retrieved, window.size());
			searchWindowQuerySide(query, window);
		}
	}

	protected final void pkwiseSearchTextSide( Record query, WindowDataset dataset ) {
		Iterable<RecordInterface> candListTextSide = getCandWindowListTextSide(query, dataset);
		for ( RecordInterface window : candListTextSide ) {
			if (rsltTextSideContains(query, window)) continue;
//			if ( window.getID() != 5189 ) continue;
			statContainer.addCount(Stat.Len_TS_Retrieved, window.size());
			searchWindowTextSide(query, window);
		}
	}
	
	@Override
	protected Iterable<RecordInterface> getCandWindowListQuerySide(Record query, WindowDataset dataset ) {
		return index.getCandWindowQuerySide(query);
	}
	
	protected Iterable<RecordInterface> getCandWindowListTextSide(Record query, WindowDataset dataset ) {
		return index.getCandWindowTextSide(query);
	}

	protected final void searchWindowTextSide(Record query, RecordInterface window) {
//		Log.log.trace("searchWindowTextSide: query=%s\t%s", ()->query, ()->query.toOriginalString());
//		Log.log.trace("searchWindowTextSide: window=%s\t%s", ()->window, ()->window.toOriginalString());
		statContainer.startWatch(Stat.Time_TS_Validation);
		statContainer.addCount(Stat.Num_TS_Verified, 1);
		double sim = Util.jaccardM(query.getTokenList(), window.getTokenList());
//		Log.log.trace("sim=%f", ()->sim);
		statContainer.stopWatch(Stat.Time_TS_Validation);
		if ( sim >= theta ) {
			addResultTextSide(query, window);
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

	public final BigInteger diskSpaceUsage() {
		return index.diskSpaceUsage();
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
		 * 1.02: use disk-based qgram index
		 * 1.03: fix OOM issue by using FileBasedLongList
		 * 1.04: remodel RecordStore
		 * 2.00: choose optimal k automatically
		 */
		return "2.00";
	}
}

package snu.kdd.faerie;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSynNaiveSearch extends AbstractSearch {

	public FaerieSynNaiveSearch(double theta) {
		super(theta);
	}

	@Override
	public void run(Dataset dataset) {
		statContainer.setAlgorithm(this);
		statContainer.mergeStatContainer(dataset.statContainer);
		statContainer.startWatch(Stat.Time_Total);
		prepareSearch(dataset);
		search(dataset);
		statContainer.stopWatch(Stat.Time_Total);
		putResultIntoStat();
		statContainer.finalizeAndOutput();
		outputResult(dataset);
	}

	protected void search( Dataset dataset ) {
		statContainer.startWatch(Stat.Time_QS_Total);
		searchQuerySide(dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
		statContainer.startWatch(Stat.Time_TS_Total);
		searchTextSide(dataset);
		statContainer.stopWatch(Stat.Time_TS_Total);
	}

	protected void searchQuerySide( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			statContainer.startWatch(Stat.Time_QS_Validation);
			for ( Record rec : dataset.getIndexedList() ) {
				for ( Record queryExp : Records.expands(query) ) {
					double sim = Util.subJaccardM(queryExp.getTokenArray(), rec.getTokenArray());
					statContainer.increment(Stat.Num_QS_Verified);
					statContainer.addCount(Stat.Len_QS_Verified, rec.size());
					if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
			statContainer.stopWatch(Stat.Time_QS_Validation);
		}
	}

	protected void searchTextSide( Dataset dataset ) {
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessAll();
			statContainer.startWatch(Stat.Time_TS_Validation);
			for ( Record query : dataset.getSearchedList() ) {
				for ( Record recExp : Records.expands(rec) ) {
					double sim = Util.subJaccardM(query.getTokenArray(), recExp.getTokenArray());
					statContainer.increment(Stat.Num_TS_Verified);
					statContainer.addCount(Stat.Len_TS_Verified, rec.size());
					if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
			statContainer.stopWatch(Stat.Time_TS_Validation);
		}
	}

	@Override
	protected void searchRecordQuerySide(Record query, Record rec) {
	}

	@Override
	protected void searchRecordTextSide(Record query, Record rec) {
	}

	@Override
	public String getName() {
		return "FaerieSynNaiveSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

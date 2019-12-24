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
		searchQuerySide(dataset);
		searchTextSide(dataset);
	}

	protected void searchQuerySide( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			for ( Record rec : dataset.getIndexedList() ) {
				for ( Record queryExp : Records.expands(query) ) {
					double sim = Util.subJaccardM(queryExp.getTokenArray(), rec.getTokenArray());
					if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
						rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
		}
	}

	protected void searchTextSide( Dataset dataset ) {
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessAll();
			for ( Record query : dataset.getSearchedList() ) {
				for ( Record recExp : Records.expands(rec) ) {
					double sim = Util.subJaccardM(query.getTokenArray(), recExp.getTokenArray());
					if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
						rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
						break;
					}
				}
			}
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

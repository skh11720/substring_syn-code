package snu.kdd.faerie;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieNaiveSearch extends AbstractSearch {

	public FaerieNaiveSearch(double theta) {
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
		for ( Record query : dataset.getSearchedList() ) {
			for ( Record rec : dataset.getIndexedList() ) {
				double sim = Util.subJaccardM(query.getTokenArray(), rec.getTokenArray());
				if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
					rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
				}
			}
		}
	}

	@Override
	protected void searchRecordQuerySide(Record query, Record rec) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void searchRecordTextSide(Record query, Record rec) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getName() {
		return "FaerieNaiveSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

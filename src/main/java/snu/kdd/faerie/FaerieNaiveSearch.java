package snu.kdd.faerie;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Util;

public class FaerieNaiveSearch extends AbstractSearch {

	public FaerieNaiveSearch(double theta) {
		super(theta);
	}

	@Override
	protected void searchRecordQuerySide(Record query, Record rec) {
		double sim = Util.subJaccardM(query.getTokenArray(), rec.getTokenArray());
		if ( sim >= theta ) {
//			Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
			addResultQuerySide(query, rec);
		}
	}

	@Override
	protected void searchRecordTextSide(Record query, Record rec) {
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

package snu.kdd.faerie;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSynNaiveSearch extends AbstractSearch {

	public FaerieSynNaiveSearch(double theta) {
		super(theta);
	}
	
	@Override
	protected void searchRecordQuerySide(Record query, Record rec) {
		statContainer.startWatch(Stat.Time_QS_Validation);
		for ( Record queryExp : Records.expands(query) ) {
			double sim = Util.subJaccardM(queryExp.getTokenArray(), rec.getTokenArray());
//			statContainer.increment(Stat.Num_QS_Verified);
//			statContainer.addCount(Stat.Len_QS_Verified, rec.size());
			if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
				addResultQuerySide(query, rec);
				break;
			}
		}
		statContainer.stopWatch(Stat.Time_QS_Validation);
	}

	@Override
	protected void searchRecordTextSide(Record query, Record rec) {
		statContainer.startWatch(Stat.Time_TS_Validation);
		rec.preprocessTransformLength();
		for ( Record recExp : Records.expands(rec) ) {
			double sim = Util.subJaccardM(query.getTokenArray(), recExp.getTokenArray());
//			statContainer.increment(Stat.Num_TS_Verified);
//			statContainer.addCount(Stat.Len_TS_Verified, rec.size());
			if ( sim >= theta ) {
//					Log.log.trace("[RESULT]"+query.getID()+"\t"+rec.getID()+"\t"+sim+"\t"+query.toOriginalString()+"\t"+rec.toOriginalString());
				addResultTextSide(query, rec);
				break;
			}
		}
		statContainer.stopWatch(Stat.Time_TS_Validation);
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

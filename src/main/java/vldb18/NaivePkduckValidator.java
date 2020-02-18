package vldb18;

import snu.kdd.substring_syn.algorithm.validator.AbstractValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class NaivePkduckValidator extends AbstractValidator {
	
//	private double sim( Record x, Record y ) {
//		log.debug(x.getID()+"\t"+y.getID());
//		if ( areSameString(x, y) ) return 1;
//		else return simx2y(x, y);
//	}
	
	public NaivePkduckValidator(double theta, StatContainer statContainer) {
		super(theta, statContainer);
		// TODO Auto-generated constructor stub
	}

	@Deprecated
	public double simx2y( Record x, Record y ) {
		if ( areSameString(x, y) ) return 1;
		double sim = 0;
		for ( Record exp : Records.expandAll(x) ) {
			sim = Math.max(sim, Util.jaccardM(exp.getTokenArray(), y.getTokenArray()));
		}
		return sim;
	}

	public boolean verifyQuerySide( Record query, RecordInterface window, double theta ) {
		statContainer.increment(Stat.Num_QS_Verified);
		statContainer.addCount(Stat.Len_QS_Verified, window.size());
		if ( areSameString(query, window) ) return true;
		for ( Record exp : Records.expands(query) ) {
			double sim = Util.subJaccardM(exp.getTokenList(), window.getTokenList());
			if ( sim >= theta ) {
//				Log.log.trace("NaivePkduckValidator.verifyQuerySide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getID(), ()->sim);
				return true;
			}
		}
		return false;
	}

	public boolean verifyTextSide( Record query, Subrecord window, double theta ) {
		statContainer.increment(Stat.Num_TS_Verified);
		statContainer.addCount(Stat.Len_TS_Verified, window.size());
		if ( areSameString(query, window) ) return true;
		Record rec = window.toRecord();
		rec.preprocessAll();
		for ( Record exp : Records.expands(rec) ) {
			double sim = Util.subJaccardM(query.getTokenList(), exp.getTokenList());
			if ( sim >= theta ) {
//				Log.log.trace("NaivePkduckValidator.verifyTextSide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getID(), ()->sim);
				return true;
			}
		}
		return false;
	}
	
	public String getName() {
		return "NaivePkduckValidator";
	}	
}

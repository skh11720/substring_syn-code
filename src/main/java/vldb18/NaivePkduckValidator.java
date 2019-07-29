package vldb18;

import snu.kdd.substring_syn.algorithm.validator.AbstractValidator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Util;

public class NaivePkduckValidator extends AbstractValidator {
	
//	private double sim( Record x, Record y ) {
//		log.debug(x.getID()+"\t"+y.getID());
//		if ( areSameString(x, y) ) return 1;
//		else return simx2y(x, y);
//	}
	
	@Deprecated
	public double simx2y( Record x, Record y ) {
		if ( areSameString(x, y) ) return 1;
		double sim = 0;
		for ( Record exp : Records.expandAll(x) ) {
			sim = Math.max(sim, Util.jaccardM(exp.getTokenArray(), y.getTokenArray()));
		}
		return sim;
	}
	
	public boolean isSimx2yOverThreahold( Record x, Record y, double theta ) {
		if ( areSameString(x, y) ) return true;
		for ( Record exp : Records.expandAll(x) ) {
			double sim = Util.jaccardM(exp.getTokenArray(), y.getTokenArray());
			if ( sim >= theta ) {
				Log.log.debug("NaivePkduckValidator.isSimx2yOverThreshold(%d, %d): sim=%.3f", ()->x.getID(), ()->y.getID(), ()->sim);
				return true;
			}
		}
		return false;
	}
	
	public String getName() {
		return "NaivePkduckValidator";
	}	
}

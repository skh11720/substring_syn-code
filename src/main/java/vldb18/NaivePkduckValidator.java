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

	public boolean verifyQuerySide( Record query, Record window, double theta ) {
		if ( areSameString(query, window) ) return true;
		for ( Record exp : Records.expandAll(query) ) {
			double sim = Util.subJaccardM(exp.getTokenList(), window.getTokenList());
			if ( sim >= theta ) {
				Log.log.debug("NaivePkduckValidator.verifyQuerySide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getID(), ()->sim);
				return true;
			}
		}
		return false;
	}

	public boolean verifyTextSide( Record query, Record window, double theta ) {
		if ( areSameString(query, window) ) return true;
		for ( Record exp : Records.expandAll(window) ) {
			double sim = Util.subJaccardM(query.getTokenList(), exp.getTokenList());
			if ( sim >= theta ) {
				Log.log.debug("NaivePkduckValidator.verifyTextSide(%d, %d): sim=%.3f", ()->query.getID(), ()->window.getID(), ()->sim);
				return true;
			}
		}
		return false;
	}
	
	public String getName() {
		return "NaivePkduckValidator";
	}	
}

package vldb18;

import snu.kdd.substring_syn.algorithm.validator.AbstractValidator;
import snu.kdd.substring_syn.data.Record;
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
		for ( Record exp : x.expandAll() ) {
			sim = Math.max(sim, Util.jaccard(exp.getTokenArray(), y.getTokenArray()));
		}
		return sim;
	}
	
	public boolean isSimx2yOverThreahold( Record x, Record y, double theta ) {
		if ( areSameString(x, y) ) return true;
		for ( Record exp : x.expandAll() ) {
			double sim = Util.jaccard(exp.getTokenArray(), y.getTokenArray());
			if ( sim >= theta ) return true;
		}
		return false;
	}
	
	public String getName() {
		return "NaivePkduckValidator";
	}	
}

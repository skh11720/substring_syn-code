package vldb18;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Util;

public class NaivePkduckValidator {
	
	static final Logger log = LogManager.getLogger("NaivePkduckValidator");
	static {
		Configurator.setLevel("NaivePkduckValidator", Level.DEBUG);
	}

	private double sim( Record x, Record y ) {
//		log.debug(x.getID()+"\t"+y.getID());
		if ( areSameString(x, y) ) return 1;
		else return simx2y(x, y);
	}
	
	private boolean areSameString( Record x, Record y ) {
		return x.equals(y);
	}
	
	public double simx2y( Record x, Record y ) {
		if ( areSameString(x, y) ) return 1;
		double sim = 0;
		for ( Record exp : x.expandAll() ) {
			sim = Math.max(sim, Util.jaccard(exp.getTokenArray(), y.getTokenArray()));
		}
		return sim;
	}
	
	public boolean isSimx2yOverThreahold( Record x, Record y, double theta ) {
		return simx2y(x, y) == theta;
	}
	
	public String getName() {
		return "NaivePkduckValidator";
	}	
}

package snu.kdd.substring_syn.data.record;

import java.util.Iterator;

import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenIndex;

public abstract class AbstractTransformableRecord implements TransformableRecordInterface {

	public static TokenIndex tokenIndex = null;

	public int getNumApplicableNonselfRules() {
		int count = 0;
		for ( Rule rule : getApplicableRuleIterable() ) {
			if( rule.isSelfRule ) continue;
			count += 1;
		}
		return count;
	}

	@Override
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for( int token : getTokenArray() ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			rslt.append(token);
		}
		return rslt.toString();
	}

	@Override
	public String toOriginalString() {
		StringBuilder rslt = new StringBuilder();
		for( int token : getTokenArray() ) {
			rslt.append(tokenIndex.getToken( token ) + " ");
		}
		return rslt.toString();
	}
	
	@Override
	public String toStringDetails() {
		StringBuilder rslt = new StringBuilder();
		rslt.append("idx: "+getIdx()+"\n");
		rslt.append("ID: "+getID()+"\n");
		rslt.append("rec: "+toOriginalString()+"\n");
		rslt.append("tokens: "+toString()+"\n");
		rslt.append("nRules: "+getNumApplicableNonselfRules()+"\n");
		for ( Rule rule : getApplicableRuleIterable() ) {
			if ( rule.isSelfRule ) continue;
			rslt.append("\t"+rule.toString()+"\t"+rule.toOriginalString()+"\n");
		}
		return rslt.toString();
	}


	class RuleIterator implements Iterator<Rule> {
		int k = -1;
		Iterator<Rule> rIter = null;
		Rule nextRule = findNext();

		@Override
		public boolean hasNext() {
			return (nextRule != null);
		}

		@Override
		public Rule next() {
			Rule rule = nextRule;
			nextRule = findNext();
			return rule;
		}
		
		private Rule findNext() {
			while (true) {
				if ( rIter == null || !rIter.hasNext() ) {
					k += 1;
					if (k < size()) rIter = getApplicableRules(k).iterator();
					else return null;
				}
				if ( rIter.hasNext() ) return rIter.next();
			}
		}
	}
}

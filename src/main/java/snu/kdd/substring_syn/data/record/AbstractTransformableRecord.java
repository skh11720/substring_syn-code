package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

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
	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			@Override
			public Iterator<Rule> iterator() {
				return new RuleIterator();
			}
		};
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
		int[] nApp = IntStream.range(0, size()).map(i->getNumApplicableRules(i)).toArray();
		int[] nSapp = IntStream.range(0, size()).map(i->getNumSuffixApplicableRules(i)).toArray();
		int[] nSRL = IntStream.range(0, size()).map(i->getNumSuffixRuleLens(i)).toArray();

		rslt.append("idx: "+getIdx()+"\n");
		rslt.append("ID: "+getID()+"\n");
		rslt.append("rec: "+toOriginalString()+"\n");
		rslt.append("tokens: "+toString()+"\n");
		rslt.append("nApp.sum: "+Arrays.stream(nApp).sum()+"\n");
		rslt.append("nApp: "+Arrays.toString(nApp)+"\n");
		rslt.append("nSapp.sum: "+Arrays.stream(nSapp).sum()+"\n");
		rslt.append("nSapp: "+Arrays.toString(nSapp)+"\n");
		rslt.append("nSRL.sum: "+Arrays.stream(nSRL).sum()+"\n");
		rslt.append("nSRL: "+Arrays.toString(nSRL)+"\n");
		rslt.append("maxRhsSize: "+getMaxRhsSize()+"\n");
		StreamSupport.stream(getApplicableRuleIterable().spliterator(), false).sorted().forEach(rule->{
			if ( !rule.isSelfRule ) rslt.append("\t"+rule.toString()+"\t"+rule.toOriginalString()+"\n");
		});
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

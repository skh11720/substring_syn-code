package snu.kdd.substring_syn.data.record;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.data.Rule;

public class Subrecord implements RecordInterface {
	
	private final Record rec;
	public final int sidx;
	public final int eidx;
	private final int hash;
	protected int maxRhsSize = 0;

	public Subrecord( RecordInterface rec, int sidx, int eidx ) {
		this.rec = rec.getSuperRecord();
		this.sidx = sidx + rec.getSidx(); // inclusive
		this.eidx = eidx + rec.getSidx(); // exclusive
		hash = getHash();
	}
	
	private int getHash() {
		int hash = rec.hashCode();
		hash = 0x1f1f1f1f ^ hash+ sidx;
		hash = 0x1f1f1f1f ^ hash+ eidx;
		return hash;
	}
	
	@Override
	public int getID() {
		return rec.getID();
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public int getSidx() {
		return sidx;
	}
	
	@Override
	public int getToken(int i) {
		return rec.getToken(sidx+i);
	}
	
	@Override
	public IntList getTokenList() {
		return rec.getTokenList().subList(sidx, eidx);
	}
	
	public int[] getTokenArray() {
		return getTokenList().toIntArray();
	}

	@Override
	public int getMaxTransLength() {
		// TODO: return more tight (accurate) value
		return rec.getMaxTransLength();
	}

	@Override
	public int size() {
		return eidx-sidx;
	}

	@Override
	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new PrefixRuleIterator();
			}
		};
	}

	@Override
	public Iterable<Rule> getApplicableRules( int i ) {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new PrefixRuleIterator(i);
			}
		};
	}
	
	public Iterable<Rule> getSuffixApplicableRuleIterable() {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new SuffixRuleIterator();
			}
		};
	}

	@Override
	public Iterable<Rule> getSuffixApplicableRules( int i ) {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new SuffixRuleIterator(i);
			}
		};
	}

	@Override
	public Iterable<Rule> getIncompatibleRules( int k ) {
		ObjectOpenHashSet<Rule> rules = new ObjectOpenHashSet<>();
		rules.addAll( new ObjectArrayList<Rule>(getApplicableRules(k).iterator()) );
		rules.addAll( new ObjectArrayList<Rule>(getSuffixApplicableRules(k).iterator()) );
		return rules;
	}
	
	@Override
	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
			maxRhsSize = 1;
			for ( int k=sidx; k<eidx; ++k ) {
				for ( Rule rule : rec.getApplicableRules(k) ) {
					if ( rule.lhsSize() <= eidx-k ) 
						maxRhsSize = Math.max(maxRhsSize, rule.rhsSize());
				}
			}
		}
		return maxRhsSize;
	}
	
	@Override
	public IntOpenHashSet getCandTokenSet() {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( int k=sidx; k<eidx; ++k ) {
			for ( Rule rule : rec.getApplicableRules(k) ) {
				if ( rule.lhsSize() <= eidx-k ) 
					tokenSet.addAll(IntArrayList.wrap(rule.getRhs()));
			}
		}
		return tokenSet;
	}
	
	@Override
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for ( int i=sidx; i<eidx; ++i ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			if (i == sidx) rslt.append("[");
			rslt.append(rec.getToken(i));
			if (i+1 == eidx) rslt.append("]");
		}
		return rslt.toString();
	}

	@Override
	public String toOriginalString() {
		StringBuilder rslt = new StringBuilder();
		for( int i=sidx; i<eidx; ++i ) {
			rslt.append(Record.tokenIndex.getToken( rec.getToken(i) ) + " ");
		}
		return rslt.toString();
	}

	public String toSuperString() {
		StringBuilder rslt = new StringBuilder();
		for ( int i=0; i<rec.size(); ++i ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			if (i == sidx) rslt.append("[");
			rslt.append(rec.getToken(i));
			if (i+1 == eidx) rslt.append("]");
		}
		return rslt.toString();
	}
	
	@Override
	public String toStringDetails() {
		return toRecord().toStringDetails();
	}
	
	public Record toRecord() {
		Record newrec = new Record(getTokenList().toIntArray());
		newrec.id = getID();
		Rule[][] applicableRules = new Rule[this.size()][];
		if ( rec.getApplicableRules() != null ) {
			for ( int k=sidx; k<eidx; ++k ) {
				ObjectArrayList<Rule> ruleList = new ObjectArrayList<>();
				for ( Rule rule : rec.getApplicableRules(k) ) {
					if ( rule.lhsSize() <= eidx-k ) ruleList.add(rule);
				}
				applicableRules[k-sidx] = new Rule[ruleList.size()];
				ruleList.toArray( applicableRules[k-sidx] );
			}
		}
		newrec.applicableRules = applicableRules;
		RecordPreprocess.preprocessSuffixApplicableRules(newrec);
		return newrec;
	}
	
	public Record getSuperRecord() {
		return rec;
	}

	abstract class RuleIterator implements Iterator<Rule> {
		final int kMax;
		int k;
		int i = 0;
		Rule[][] rules;
		
		RuleIterator() {
			k = sidx;
			kMax = eidx;
		}

		RuleIterator( int k ) {
			this.k = sidx+k;
			kMax = this.k+1;
		}
		
		@Override
		public boolean hasNext() {
			return (k < kMax);
		}

		@Override
		public Rule next() {
			Rule rule = rules[k][i++];
			findNext();
			return rule;
		}
		
		void findNext() {
			while ( k < kMax) {
				while ( i < rules[k].length ) {
					if ( isValid(rules[k][i]) ) return;
					++i;
				}
				++k;
				i = 0;
			}
		}
		
		abstract boolean isValid( Rule rule );
	}

	class PrefixRuleIterator extends RuleIterator {
		
		PrefixRuleIterator() {
			rules = rec.applicableRules;
			findNext();
		}
		
		PrefixRuleIterator( int k ) {
			this();
			findNext();
		}
		
		@Override
		boolean isValid(Rule rule) {
			return k+rule.lhsSize() <= eidx;
		}
	}
	
	class SuffixRuleIterator extends RuleIterator {
		
		SuffixRuleIterator() {
			rules = rec.suffixApplicableRules;
			findNext();
		}
		
		SuffixRuleIterator( int k ) {
			super(k);
			rules = rec.suffixApplicableRules;
			findNext();
		}
		
		@Override
		boolean isValid(Rule rule) {
			return k-rule.lhsSize()+1 >= sidx;
		}
	}
}

package snu.kdd.substring_syn.data.record;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.utils.Util;

public class Subrecord implements RecordInterface {
	
	protected final Record rec;
	public final int sidx;
	public final int eidx;
	protected final int hash;
	protected int maxRhsSize = 0;

	public Subrecord( RecordInterface rec, int sidx, int eidx ) {
		this.rec = rec.getSuperRecord();
		this.sidx = sidx + rec.getSidx(); // inclusive
		this.eidx = eidx + rec.getSidx(); // exclusive
		hash = getHash();
	}
	
	protected int getHash() {
		// djb2-like
		int hash = Util.bigprime + rec.getID();
		for( int i=sidx; i<eidx; ++i ) {
			int token = rec.tokens[i];
			hash = ( hash << 5 ) + Util.bigprime + token;
//                tmp = 0x1f1f1f1f ^ tmp + token;
//			hash = hash % Util.bigprime;
		}
		return (int) ( hash % Integer.MAX_VALUE );
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
	
	public Record getSuperRecord() {
		return rec;
	}

	abstract class RuleIterator implements Iterator<Rule> {
		final int kMax;
		int k;
		int i = 0;
		Rule[][] rules;
		
		RuleIterator() {
			this(sidx, eidx);
		}

		RuleIterator( int k ) {
			this(sidx+k, sidx+k+1);
		}
		
		RuleIterator( int k, int kMax ) {
			this.k = k;
			this.kMax = kMax;
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
			super(k);
			rules = rec.applicableRules;
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
	
	public Record toRecord() {
		Record newrec = new Record(this.getTokenList().toIntArray());
		newrec.id = this.getID();
		int maxRhsSize = rec.getMaxRhsSize();
		Rule[][] applicableRules = null;
		if ( this.rec.getApplicableRules() != null ) {
			applicableRules = new Rule[this.size()][];
			int k = this.sidx;
			for ( ; k<this.eidx-maxRhsSize-1; ++k ) applicableRules[k-this.sidx] = rec.applicableRules[k];
			for ( ; k<this.eidx; ++k ) {
				ObjectArrayList<Rule> ruleList = new ObjectArrayList<>(this.getApplicableRules(k-this.sidx).iterator());
				applicableRules[k-this.sidx] = new Rule[ruleList.size()];
				ruleList.toArray( applicableRules[k-this.sidx] );
			}
        }
		newrec.applicableRules = applicableRules;
		newrec.preprocessSuffixApplicableRules();
		return newrec;
	}
	
//    public static Record toRecord( Subrecord subrec, TransLenCalculator transLen ) {
//        Record newrec = toRecord(subrec);
//        int[][] transformLengths = null;
//        if ( transLen != null ) {
//            transformLengths = new int[subrec.size()][2];
//            for ( int i=0; i<subrec.size(); ++i ) {
//                transformLengths[i][0] = transLen.getLB(subrec.sidx, subrec.sidx+i);
//                transformLengths[i][1] = transLen.getUB(subrec.sidx, subrec.sidx+i);
//            }
//        }
//        newrec.transformLengths = transformLengths;
//        return newrec;
//    }
}

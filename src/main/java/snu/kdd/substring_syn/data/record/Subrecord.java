package snu.kdd.substring_syn.data.record;

import java.util.Iterator;
import java.util.stream.StreamSupport;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.utils.Util;

public class Subrecord implements TransformableRecordInterface, RecursiveRecordInterface {
	
	protected final TransformableRecordInterface rec;
	public final int sidx;
	public final int eidx;
	protected final int hash;
	protected int maxRhsSize = 0;
	protected int maxTransLen = 0;

	public Subrecord( RecordInterface rec, int sidx, int eidx ) {
		// TODO: remove the cast
		this.rec = ((RecursiveRecordInterface)rec).getSuperRecord();
		this.sidx = sidx + ((RecursiveRecordInterface)rec).getSidx(); // inclusive
		this.eidx = eidx + ((RecursiveRecordInterface)rec).getSidx(); // exclusive
		hash = getHash();
	}
	
	protected int getHash() {
		// djb2-like
		int hash = Util.bigprime + rec.getIdx();
		for( int i=sidx; i<eidx; ++i ) {
			int token = rec.getToken(i);
			hash = ( hash << 5 ) + Util.bigprime + token;
//                tmp = 0x1f1f1f1f ^ tmp + token;
//			hash = hash % Util.bigprime;
		}
		return (int) ( hash % Integer.MAX_VALUE );
	}
	
	@Override
	public int getIdx() {
		return rec.getIdx();
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
	public int getNumApplicableRules(int i) {
		return (int)StreamSupport.stream(getApplicableRules(i).spliterator(), true).count();
	}
	
	@Override
	public int getNumSuffixApplicableRules(int i) {
		return (int)StreamSupport.stream(getSuffixApplicableRules(i).spliterator(), true).count();
	}
	
	@Override
	public int getNumSuffixRuleLens(int i) {
		//TODO: subrec.suffixRuleLens should be computed exactly
		return rec.getNumSuffixRuleLens(i);
	}

	@Override
	public final int getMaxTransLength() {
		if ( maxTransLen == 0 ) preprocessTransformLength();
		return maxTransLen;
	}

	private void preprocessTransformLength() {
		TransLenLazyCalculator cal = new TransLenLazyCalculator(null, this, 0, size(), 0);
		maxTransLen = cal.getUB(size()-1);
	}

	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new PrefixRuleIterator();
			}
		};
	}

	public Iterable<Rule> getApplicableRules( int i ) {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new PrefixRuleIterator(i);
			}
		};
	}
	
//	public Iterable<Rule> getSuffixApplicableRuleIterable() {
//		return new Iterable<Rule>() {
//			
//			@Override
//			public Iterator<Rule> iterator() {
//				return new SuffixRuleIterator();
//			}
//		};
//	}

	public Iterable<Rule> getSuffixApplicableRules( int i ) {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new SuffixRuleIterator(i);
			}
		};
	}
	
	public Iterable<IntPair> getSuffixRuleLens( int k ) {
		//TODO: subrec.suffixRuleLens should be computed exactly
		return rec.getSuffixRuleLens(sidx+k);
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
	
	@Override
	public TransformableRecordInterface getSuperRecord() {
		return rec;
	}

	abstract class RuleIterator implements Iterator<Rule> {
		final int kMax;
		int k;
		Rule nextRule = null;
		Iterator<Rule> rIter = null;
		
		RuleIterator() {
			this(sidx, eidx);
		}

		RuleIterator( int k ) {
			this(sidx+k, sidx+k+1);
		}
		
		RuleIterator( int k, int kMax ) {
			this.k = k;
			this.kMax = kMax;
			rIter = getRuleIterator(k);
		}
		
		abstract Iterator<Rule> getRuleIterator(int k);
		
		@Override
		public boolean hasNext() {
			return (k < kMax);
		}

		@Override
		public Rule next() {
			Rule rule = nextRule;
			findNext();
			return rule;
		}
		
		void findNext() {
			while ( k < kMax) {
				while ( rIter.hasNext() ) {
					nextRule = rIter.next();
					if ( isValid(nextRule) ) return;
				}
				++k;
				if ( k < kMax ) rIter = getRuleIterator(k);
				else break;
			}
			nextRule = null;
		}
		
		abstract boolean isValid( Rule rule );
	}

	class PrefixRuleIterator extends RuleIterator {
		
		PrefixRuleIterator() {
			findNext();
		}
		
		PrefixRuleIterator( int k ) {
			super(k);
			findNext();
		}
		
		@Override
		Iterator<Rule> getRuleIterator(int k) {
			return rec.getApplicableRules(k).iterator();
		}
		
		@Override
		boolean isValid(Rule rule) {
			return k+rule.lhsSize() <= eidx;
		}
	}
	
	class SuffixRuleIterator extends RuleIterator {
		
		SuffixRuleIterator() {
			findNext();
		}
		
		SuffixRuleIterator( int k ) {
			super(k);
			findNext();
		}
		
		@Override
		Iterator<Rule> getRuleIterator(int k) {
			return rec.getSuffixApplicableRules(k).iterator();
		}
		
		@Override
		boolean isValid(Rule rule) {
			return k-rule.lhsSize()+1 >= sidx;
		}
	}
	
	public Record toRecord() {
		Record newrec = new Record(this.getIdx(), this.getID(), this.getTokenList().toIntArray());

		Rule[][] applicableRules = null;
		applicableRules = new Rule[size()][];
		for ( int i=0; i<size(); ++i ) {
			applicableRules[i] = (new ObjectArrayList<Rule>(getApplicableRules(i).iterator())).toArray(new Rule[0]);
		}
        
		newrec.applicableRules = applicableRules;

		Rule[][] suffixApplicableRules = null;
		suffixApplicableRules = new Rule[size()][];
		for ( int i=0; i<size(); ++i ) {
			suffixApplicableRules[i] = (new ObjectArrayList<Rule>(getSuffixApplicableRules(i).iterator())).toArray(new Rule[0]);
		}
		
		newrec.suffixApplicableRules = suffixApplicableRules;

		IntPair[][] suffixRuleLenPairs = null;
		suffixRuleLenPairs = new IntPair[size()][];
		for ( int i=0; i<size(); ++i ) {
			ObjectSet<IntPair> pairSet = new ObjectOpenHashSet<>();
			for ( Rule rule : suffixApplicableRules[i] ) pairSet.add(new IntPair(rule.lhsSize(), rule.rhsSize()));
			suffixRuleLenPairs[i] = pairSet.toArray( new IntPair[0] );
		}
		
		newrec.suffixRuleLenPairs = suffixRuleLenPairs;
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

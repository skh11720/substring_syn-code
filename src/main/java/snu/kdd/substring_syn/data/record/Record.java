package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.utils.Util;

public class Record implements TransformableRecordInterface, RecursiveRecordInterface, Comparable<Record> {
	
	public static final Record EMPTY_RECORD = new Record(new int[0]);
	public static TokenIndex tokenIndex = null;

	/*
	 * idx+1 == id if we use all records in the data file
	 */
	protected final int idx; // incremental, nonnegative, zero-based, its order in RecordStore
	protected final int id; // not necessarily incremental, nonnegative, line number of this record in the data file
	protected final int[] tokens;
	protected final int hash;

	Rule[][] applicableRules = null;
	Rule[][] suffixApplicableRules = null;
//	int[][] transformLengths = null;
//	long[] estTrans;
	IntPair[][] suffixRuleLenPairs = null;

	int maxTransLen = 0;
	int minTransLen = 0;
	int maxRhsSize = 0;
	

	public Record( int idx, int id, String str ) {
		this.idx = idx;
		this.id = id;
		String[] pstr = Records.tokenize(str);
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getIDOrAdd( pstr[ i ] );
		}
		
		hash = getHash(idx, tokens, tokens.length);
	}
	
	public Record( int idx, int id, int[] tokens ) {
		this.idx = idx;
		this.id = id;
		this.tokens = tokens;
		hash = getHash(idx, tokens, tokens.length);
	}

	public Record( int[] tokens ) { // for transformed strings
		this(-1, -1, tokens);
	}

	public int getIdx() {
		return idx;
	}
	
	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals( Object o ) {
		if( o != null ) {
			Record orec = (Record) o;
			if( this == orec ) return true;
			if( idx == orec.idx || idx == -1 || orec.idx == -1 ) {
				return Records.compare( tokens, orec.tokens ) == 0;
			}
			return false;
		}
		else return false;
	}

	public Collection<Integer> getTokens() {
		return IntArrayList.wrap(tokens);
	}

	public IntSet getDistinctTokens() {
		return new IntOpenHashSet(tokens);
	}

	public int[] getTokenArray() {
		return tokens;
	}

	public IntList getTokenList() {
		return IntArrayList.wrap(tokens);
	}

	public int getToken( int i ) {
		return tokens[i];
	}
	
	@Override
	public int getSidx() {
		return 0;
	}
	
	@Override
	public int compareTo( Record o ) {
		if( tokens.length != o.tokens.length ) {
			return tokens.length - o.tokens.length;
		}

		int idx = 0;
		while( idx < tokens.length ) {
			int cmp = Integer.compare( tokens[ idx ], o.tokens[ idx ] );
			if( cmp != 0 ) {
				return cmp;
			}
			++idx;
		}
		return 0;
	}

	public int size() {
		return tokens.length;
	}
	
	public Record getSuperRecord() {
		return this;
	}
	
	public int getNumApplicableRules() {
		return Arrays.stream(applicableRules).mapToInt(x->x.length).sum();
	}

	public int getNumApplicableRules(int pos) {
		return applicableRules[pos].length;
	}
	
	public int getNumApplicableNonselfRules() {
		int count = 0;
		for ( Rule rule : getApplicableRuleIterable() ) {
			if( rule.isSelfRule ) continue;
			count += 1;
		}
		return count;
	}
	
	public Rule getRule(int pos, int idx) {
		return applicableRules[pos][idx];
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
	public Iterable<Rule> getApplicableRules( int k ) {
		if( applicableRules == null ) {
			return null;
		}
		else if( k < applicableRules.length ) {
			return Arrays.asList(applicableRules[k]);
		}
		else {
			return Arrays.asList(Rule.EMPTY_RULE);
		}
	}

	@Override
	public Iterable<Rule> getSuffixApplicableRules( int k ) {
		if( suffixApplicableRules == null ) {
			return null;
		}
		else if( k < suffixApplicableRules.length ) {
			return Arrays.asList(suffixApplicableRules[k]);
		}
		else {
			return Arrays.asList(Rule.EMPTY_RULE);
		}
	}
	
	public IntPair[] getSuffixRuleLens( int k ) {
		if ( suffixRuleLenPairs == null ) {
			return null;
		}
		else if ( k < suffixRuleLenPairs.length ) {
			return suffixRuleLenPairs[k];
		}
		else return null;
	}

	public final int getMaxTransLength() {
		if ( maxTransLen == 0 ) preprocessTransformLength();
		return maxTransLen;
	}

	public final int getMinTransLength() {
		if ( minTransLen == 0 ) preprocessTransformLength();
		return minTransLen;
	}
	
	private void preprocessTransformLength() {
		TransLenLazyCalculator cal = new TransLenLazyCalculator(null, this, 0, size(), 0);
		maxTransLen = cal.getUB(size()-1);
		minTransLen = cal.getLB(size()-1);
	}

	@Override
	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
			maxRhsSize = 1;
			for ( int k=0; k<tokens.length; ++k) {
				for ( IntPair pair : suffixRuleLenPairs[k] ) {
					maxRhsSize = Math.max(maxRhsSize, pair.i2);
				}
			}
		}
		return maxRhsSize;
	}
	
	public final IntOpenHashSet getCandTokenSet() {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Rule r : getApplicableRuleIterable() ) {
			for ( int token : r.getRhs() ) tokenSet.add(token);
		}
		return tokenSet;
	}
	
	public void preprocessAll() {
		preprocessApplicableRules();
		preprocessSuffixApplicableRules();
		preprocessTransformLength();
	}
	
	public void preprocessApplicableRules() {
		if ( applicableRules != null ) return;
		applicableRules = Rule.automata.applicableRules(tokens);
	}

	public void preprocessSuffixApplicableRules() {
		if ( suffixApplicableRules != null ) return;
		ObjectList<ObjectList<Rule>> tmplist = new ObjectArrayList<ObjectList<Rule>>();
		ObjectList<ObjectSet<IntPair>> pairList = new ObjectArrayList<>();

		for( int i = 0; i < tokens.length; ++i ) {
			tmplist.add( new ObjectArrayList<Rule>() );
			pairList.add( new ObjectOpenHashSet<>() );
		}

		for( int i = tokens.length - 1; i >= 0; --i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int suffixidx = i + rule.getLhs().length - 1;
				tmplist.get( suffixidx ).add( rule );
				pairList.get( suffixidx ).add( new IntPair(rule.lhsSize(), rule.rhsSize()) );
			}
		}

		suffixApplicableRules = new Rule[ tokens.length ][];
		suffixRuleLenPairs = new IntPair[ tokens.length ][];
		for( int i = 0; i < tokens.length; ++i ) {
			suffixApplicableRules[ i ] = tmplist.get( i ).toArray( new Rule[ 0 ] );
			suffixRuleLenPairs[i] = pairList.get(i).toArray( new IntPair[0] );
		}
	}

//	public void preprocessTransformLength() {
//		if ( transformLengths != null ) return;
//		transformLengths = new int[ tokens.length ][ 2 ];
//		for( int i = 0; i < tokens.length; ++i )
//			transformLengths[ i ][ 0 ] = transformLengths[ i ][ 1 ] = i + 1;
//
//		for( Rule rule : applicableRules[ 0 ] ) {
//			int fromSize = rule.lhsSize();
//			int toSize = rule.rhsSize();
//			if( fromSize > toSize ) {
//				transformLengths[ fromSize - 1 ][ 0 ] = Math.min( transformLengths[ fromSize - 1 ][ 0 ], toSize );
//			}
//			else if( fromSize < toSize ) {
//				transformLengths[ fromSize - 1 ][ 1 ] = Math.max( transformLengths[ fromSize - 1 ][ 1 ], toSize );
//			}
//		}
//		for( int i = 1; i < tokens.length; ++i ) {
//			transformLengths[ i ][ 0 ] = Math.min( transformLengths[ i ][ 0 ], transformLengths[ i - 1 ][ 0 ] + 1 );
//			transformLengths[ i ][ 1 ] = Math.max( transformLengths[ i ][ 1 ], transformLengths[ i - 1 ][ 1 ] + 1 );
//			for( Rule rule : applicableRules[ i ] ) {
//				int fromSize = rule.lhsSize();
//				int toSize = rule.rhsSize();
//				if( fromSize > toSize ) {
//					transformLengths[ i + fromSize - 1 ][ 0 ] = Math.min( transformLengths[ i + fromSize - 1 ][ 0 ],
//							transformLengths[ i - 1 ][ 0 ] + toSize );
//				}
//				else if( fromSize < toSize ) {
//					transformLengths[ i + fromSize - 1 ][ 1 ] = Math.max( transformLengths[ i + fromSize - 1 ][ 1 ],
//							transformLengths[ i - 1 ][ 1 ] + toSize );
//				}
//
//			}
//		}
//	}
	
	@Override
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for( int token : tokens ) {
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
		for( int token : tokens ) {
			rslt.append(tokenIndex.getToken( token ) + " ");
		}
		return rslt.toString();
	}
	
	@Override
	public String toStringDetails() {
		StringBuilder rslt = new StringBuilder();
		rslt.append("idx: "+idx+"\n");
		rslt.append("ID: "+id+"\n");
		rslt.append("rec: "+toOriginalString()+"\n");
		rslt.append("tokens: "+toString()+"\n");
		rslt.append("nRules: "+getNumApplicableNonselfRules()+"\n");
		for ( Rule rule : getApplicableRuleIterable() ) {
			if ( rule.isSelfRule ) continue;
			rslt.append("\t"+rule.toString()+"\t"+rule.toOriginalString()+"\n");
		}
		return rslt.toString();
	}

	static int getHash(int idx, int[] tokens, int size) {
		// djb2-like
		int hash = Util.bigprime + idx;
		for ( int i=0; i<size; ++i ) {
			hash = ( hash << 5 ) + Util.bigprime + tokens[i];
//                tmp = 0x1f1f1f1f ^ tmp + token;
//			hash = hash % Util.bigprime;
		}
		return (int) ( hash % Integer.MAX_VALUE );
	}



	
	
	class RuleIterator implements Iterator<Rule> {
		int k = 0;
		int i = 0;

		@Override
		public boolean hasNext() {
			return (k < applicableRules.length);
		}

		@Override
		public Rule next() {
			Rule rule = applicableRules[k][i++];
			if ( i >= applicableRules[k].length ) {
				++k;
				i = 0;
			}
			return rule;
		}
	}
}

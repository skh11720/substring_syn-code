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
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.utils.Util;

public class Record implements RecordInterface, Comparable<Record> {
	
	public static final Record EMPTY_RECORD = new Record(new int[0]);
	public static TokenIndex tokenIndex = null;

	int id;
	int[] tokens;
	int hash;

	Rule[][] applicableRules = null;
	Rule[][] suffixApplicableRules = null;
	Rule[][] suffixNonselfApplicableRules = null;
	int[][] transformLengths = null;
//	long[] estTrans;
	IntPair[][] suffixRuleLenPairs = null;

	int maxRhsSize = 0;
	int transSetLB = 0;
	
	public Record( int id, String str ) {
		this.id = id;
		String[] pstr = Records.tokenize(str);
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getIDOrAdd( pstr[ i ] );
		}
		
		hash = getHash();
	}
	
	public Record( int id, int[] tokens ) {
		this.id = id;
		this.tokens = tokens;
		hash = getHash();
	}

	public Record( int[] tokens ) { // for transformed strings
		this(-1, tokens);
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
			if( id == orec.id || id == -1 || orec.id == -1 ) {
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
	
	public void setToken( int token, int i ) {
		tokens[i] = token;
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
	
	public int getTransSetLB() {
		if ( transSetLB == 0 ) transSetLB = Records.getTransSetSizeLowerBound(this);
		return transSetLB;
	}

	public int getNumApplicableRules() {
		int count = 0;
		for( int i = 0; i < applicableRules.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				if( rule.isSelfRule ) {
					continue;
				}
				++count;
			}
		}
		return count;
	}

	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			@Override
			public Iterator<Rule> iterator() {
				return new RuleIterator();
			}
		};
	}

	public Rule[][] getApplicableRules() {
		return applicableRules;
	}
	
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

	public Rule[][] getSuffixApplicableRules() {
		return suffixApplicableRules;
	}

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

	public int getMaxTransLength() {
		return transformLengths[ tokens.length - 1 ][ 1 ];
	}

	public int getMinTransLength() {
		return transformLengths[ tokens.length - 1 ][ 0 ];
	}

	@Override
	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
			maxRhsSize = 1;
			for ( Rule rule : getApplicableRuleIterable() ) {
				maxRhsSize = Math.max(maxRhsSize, rule.rhsSize());
			}
		}
		return maxRhsSize;
	}
	
	public Iterable<Rule> getIncompatibleRules( int k ) {
		ObjectOpenHashSet<Rule> rules = new ObjectOpenHashSet<>();
		rules.addAll( Arrays.asList(applicableRules[k]) );
		rules.addAll( Arrays.asList(suffixApplicableRules[k]) );
		return rules;
	}
	
	@Override
	public IntOpenHashSet getCandTokenSet() {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Rule r : getApplicableRuleIterable() ) {
			for ( int token : r.getRhs() ) tokenSet.add(token);
		}
		return tokenSet;
	}
	
	public Record getSubrecord( int sidx, int eidx ) {
		Record newrec = new Record(getTokenList().subList(sidx, eidx).toIntArray());
		newrec.id = getID();
		newrec.applicableRules = null;
		newrec.suffixApplicableRules = null;
		newrec.transformLengths = null;
		newrec.suffixRuleLenPairs = null;
		return newrec;
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

	protected void preprocessTransformLength() {
		if ( transformLengths != null ) return;
		transformLengths = new int[ tokens.length ][ 2 ];
		for( int i = 0; i < tokens.length; ++i )
			transformLengths[ i ][ 0 ] = transformLengths[ i ][ 1 ] = i + 1;

		for( Rule rule : applicableRules[ 0 ] ) {
			int fromSize = rule.lhsSize();
			int toSize = rule.rhsSize();
			if( fromSize > toSize ) {
				transformLengths[ fromSize - 1 ][ 0 ] = Math.min( transformLengths[ fromSize - 1 ][ 0 ], toSize );
			}
			else if( fromSize < toSize ) {
				transformLengths[ fromSize - 1 ][ 1 ] = Math.max( transformLengths[ fromSize - 1 ][ 1 ], toSize );
			}
		}
		for( int i = 1; i < tokens.length; ++i ) {
			transformLengths[ i ][ 0 ] = Math.min( transformLengths[ i ][ 0 ], transformLengths[ i - 1 ][ 0 ] + 1 );
			transformLengths[ i ][ 1 ] = Math.max( transformLengths[ i ][ 1 ], transformLengths[ i - 1 ][ 1 ] + 1 );
			for( Rule rule : applicableRules[ i ] ) {
				int fromSize = rule.lhsSize();
				int toSize = rule.rhsSize();
				if( fromSize > toSize ) {
					transformLengths[ i + fromSize - 1 ][ 0 ] = Math.min( transformLengths[ i + fromSize - 1 ][ 0 ],
							transformLengths[ i - 1 ][ 0 ] + toSize );
				}
				else if( fromSize < toSize ) {
					transformLengths[ i + fromSize - 1 ][ 1 ] = Math.max( transformLengths[ i + fromSize - 1 ][ 1 ],
							transformLengths[ i - 1 ][ 1 ] + toSize );
				}

			}
		}
	}
	
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for( int id : tokens ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			rslt.append(id);
		}
		return rslt.toString();
	}

	public String toOriginalString() {
		StringBuilder rslt = new StringBuilder();
		for( int id : tokens ) {
			rslt.append(tokenIndex.getToken( id ) + " ");
		}
		return rslt.toString();
	}
	
	public String toStringDetails() {
		StringBuilder rslt = new StringBuilder();
		rslt.append("ID: "+id+"\n");
		rslt.append("rec: "+toOriginalString()+"\n");
		rslt.append("tokens: "+toString()+"\n");
		rslt.append("nRules: "+getNumApplicableRules()+"\n");
		for ( Rule rule : getApplicableRuleIterable() ) {
			if ( rule.isSelfRule ) continue;
			rslt.append("\t"+rule.toString()+"\t"+rule.toOriginalString()+"\n");
		}
		return rslt.toString();
	}

	private int getHash() {
		// djb2-like
		int hash = Util.bigprime + id;
		for( int token : tokens ) {
			hash = ( hash << 5 ) + Util.bigprime + token;
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

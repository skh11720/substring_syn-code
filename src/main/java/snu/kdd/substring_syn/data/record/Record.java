package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.utils.Util;

public class Record implements RecordInterface, Comparable<Record> {
	
	public static final Record EMPTY_RECORD = new Record(new int[0]);
	public static TokenIndex tokenIndex = null;

	int id;
	int[] tokens;
	int num_dist_tokens;
	int hash;

	Rule[][] applicableRules = null;
	Rule[][] suffixApplicableRules = null;
	int[][] transformLengths = null;
//	long[] estTrans;

	int maxRhsSize = 0;
	int transSetLB = 0;
	
	public Record( int id, String str, TokenIndex tokenIndex ) {
		this.id = id;
		String[] pstr = str.split( "( |\t)+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getIDOrAdd( pstr[ i ] );
		}
		
		num_dist_tokens = new IntOpenHashSet( tokens ).size();
		hash = getHash();
	}
	
	public Record( int id, int[] tokens ) {
		this.id = id;
		this.tokens = tokens;
		if ( tokens != null ) num_dist_tokens = new IntOpenHashSet( tokens ).size();
		else num_dist_tokens = 0;
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
				if( id == -1 || orec.id == -1 ) {
					return Records.compare( tokens, orec.tokens ) == 0;
				}
				return true;
			}
			return false;
		}
		else return false;
	}

	public Collection<Integer> getTokens() {
		return IntArrayList.wrap(tokens);
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
	
	public int getDistinctTokenCount() {
		return num_dist_tokens;
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
	
	public int[][] getTransLengthsAll() {
		return transformLengths;
	}

	public int[] getTransLengths() {
		return transformLengths[ tokens.length - 1 ];
	}

	public int getMaxTransLength() {
		return transformLengths[ tokens.length - 1 ][ 1 ];
	}

	public int getMinTransLength() {
		return transformLengths[ tokens.length - 1 ][ 0 ];
	}

	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
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
			tokenSet.addAll(IntArrayList.wrap(r.getRhs()));
		}
		return tokenSet;
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
		int hash = 0;
		for( int token : tokens ) {
			hash = ( hash << 32 ) + token;
//                tmp = 0x1f1f1f1f ^ tmp + token;
			hash = hash % Util.bigprime;
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

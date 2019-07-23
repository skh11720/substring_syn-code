package snu.kdd.substring_syn.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.utils.Util;

public class Record implements RecordInterface, Comparable<Record> {
	
	public static final Record EMPTY_RECORD = new Record(new int[0]);
	public static TokenIndex tokenIndex = null;

	protected int id;
	protected int[] tokens;
	protected int num_dist_tokens;
	protected int hash;

	protected Rule[][] applicableRules = null;
	protected Rule[][] suffixApplicableRules = null;
	protected int[][] transformLengths = null;
	protected long[] estTrans;

	protected int maxRhsSize = 0;
	protected int transSetLB = 0;
	
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
		if (tokens != null ) num_dist_tokens = new IntOpenHashSet( tokens ).size();
		else num_dist_tokens = 0;
		hash = getHash();
	}

	public Record( int[] tokens ) { // for transformed strings
		this(-1, tokens);
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

	public int[] getTokenArray() {
		return tokens;
	}

	public IntList getTokenList() {
		return IntArrayList.wrap(tokens);
	}

	public int getDistinctTokenCount() {
		return num_dist_tokens;
	}
	
	public int getToken( int i ) {
		return tokens[i];
	}
	
	public Record getSuperRecord() {
		return this;
	}
	
	@Override
	public int getSidx() {
		return 0;
	}
	
	public int getTransSetLB() {
		if ( transSetLB == 0 ) transSetLB = Records.getTransSetSizeLowerBound(this);
		return transSetLB;
	}

	/**
	 * Set applicable rules
	 */

	public void preprocessApplicableRules( ACAutomataR automata ) {
		applicableRules = automata.applicableRules( tokens );
	}
	
	public void setApplicableRules( Rule[][] applicableRules ) {
		this.applicableRules = applicableRules;
	}

	public Rule[][] getApplicableRules() {
		return applicableRules;
	}
	
	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			@Override
			public Iterator<Rule> iterator() {
				return new RuleIterator();
			}
		};
	}

	public Rule[] getApplicableRules( int k ) {
		if( applicableRules == null ) {
			return null;
		}
		else if( k < applicableRules.length ) {
			return applicableRules[ k ];
		}
		else {
			return Rule.EMPTY_RULE;
		}
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

	/**
	 * Set and return estimated number of transformed strings of the string
	 */

	public void preprocessEstimatedRecords() {
		@SuppressWarnings( "unchecked" )
		ArrayList<Rule>[] tmpAppRules = new ArrayList[ tokens.length ];
		for( int i = 0; i < tokens.length; ++i )
			tmpAppRules[ i ] = new ArrayList<Rule>();

		for( int i = 0; i < tokens.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int eidx = i + rule.lhsSize() - 1;
				tmpAppRules[ eidx ].add( rule );
			}
		}

		long[] est = new long[ tokens.length ];
		estTrans = est;
		for( int i = 0; i < est.length; ++i ) {
			est[ i ] = Long.MAX_VALUE;
		}

		for( int i = 0; i < tokens.length; ++i ) {
			long size = 0;
			for( Rule rule : tmpAppRules[ i ] ) {
				int sidx = i - rule.lhsSize() + 1;
				if( sidx == 0 ) {
					size += 1;
				}
				else {
					size += est[ sidx - 1 ];
				}

				if( size < 0 ) {
					return;
				}
			}
			est[ i ] = size;
		}
	}

	public long[] getEstNumTransformedArray() {
		return estTrans;
	}

	public long getEstNumTransformed() {
		return estTrans[ estTrans.length - 1 ];
	}

	/**
	 * Expand this record with preprocessed rules
	 */

	public ArrayList<Record> expandAll() {
		ArrayList<Record> rslt = new ArrayList<Record>();
		expandAll( rslt, 0, this.tokens );
		return rslt;
	}

	protected void expandAll( ArrayList<Record> rslt, int idx, int[] t ) {

		Rule[] rules = applicableRules[ idx ];

		for( Rule rule : rules ) {
			if( rule.isSelfRule ) {
				if( idx + 1 != tokens.length ) {
					expandAll( rslt, idx + 1, t );
				}
				else {
					rslt.add( new Record( t ) );
				}
			}
			else {
				int newSize = t.length - rule.lhsSize() + rule.rhsSize();

				int[] new_rec = new int[ newSize ];

				int rightSize = tokens.length - idx;
				int rightMostSize = rightSize - rule.lhsSize();

				int[] rhs = rule.getRhs();

				int k = 0;
				for( int i = 0; i < t.length - rightSize; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}
				for( int i = 0; i < rhs.length; i++ ) {
					new_rec[ k++ ] = rhs[ i ];
				}
				for( int i = t.length - rightMostSize; i < t.length; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}

				int new_idx = idx + rule.lhsSize();
				if( new_idx == tokens.length ) {
					rslt.add( new Record( new_rec ) );
				}
				else {
					expandAll( rslt, new_idx, new_rec );
				}
			}
		}
	}

	public void preprocessTransformLength() {
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

	public int getID() {
		return id;
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

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals( Object o ) {
		if( o != null ) {
			Record orec = (Record) o;

			if( this == orec ) {
				return true;
			}
			if( id == orec.id || id == -1 || orec.id == -1 ) {
				if( id == -1 || orec.id == -1 ) {
					return Record.compare( tokens, orec.tokens ) == 0;
				}
				return true;
			}

			return false;
		}
		else {
			return false;
		}
	}

	/* Get/set suffix applicable rules for validators */

	public void preprocessSuffixApplicableRules() {
		List<List<Rule>> tmplist = new ArrayList<List<Rule>>();

		for( int i = 0; i < tokens.length; ++i ) {
			tmplist.add( new ArrayList<Rule>() );
		}

		for( int i = tokens.length - 1; i >= 0; --i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int suffixidx = i + rule.getLhs().length - 1;
				tmplist.get( suffixidx ).add( rule );
			}
		}

		suffixApplicableRules = new Rule[ tokens.length ][];
		for( int i = 0; i < tokens.length; ++i ) {
			suffixApplicableRules[ i ] = tmplist.get( i ).toArray( new Rule[ 0 ] );
		}
	}
	
	public Rule[][] getSuffixApplicableRules() {
		return suffixApplicableRules;
	}

	public Rule[] getSuffixApplicableRules( int k ) {
		if( suffixApplicableRules == null ) {
			return null;
		}
		else if( k < suffixApplicableRules.length ) {
			return suffixApplicableRules[ k ];
		}
		else {
			return Rule.EMPTY_RULE;
		}
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
	
	public int size() {
		return tokens.length;
	}

	public Collection<Integer> getTokens() {
		return IntArrayList.wrap(tokens);
	}

	public static int compare( int[] str1, int[] str2 ) {
		if( str1.length == 0 || str2.length == 0 ) {
			return str1.length - str2.length;
		}

		int idx = 0;
		int lastcmp = 0;

		while( idx < str1.length && idx < str2.length && ( lastcmp = Integer.compare( str1[ idx ], str2[ idx ] ) ) == 0 ) {
			++idx;
		}

		if( lastcmp != 0 ) {
			return lastcmp;
		}
		else if( str1.length == str2.length ) {
			return 0;
		}
		else if( idx == str1.length ) {
			return -1;
		}
		else {
			return 1;
		}
	}
	
	public void reindex( TokenOrder order ) {
		for ( int i=0; i<tokens.length; ++i ) {
			tokens[i] = order.getOrder(tokens[i]);
		}
	}
	
	public IntOpenHashSet getCandTokenSet() {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Rule r : getApplicableRuleIterable() ) {
			tokenSet.addAll(IntArrayList.wrap(r.getRhs()));
		}
		return tokenSet;
	}
	
	
	
	protected class RuleIterator implements Iterator<Rule> {
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

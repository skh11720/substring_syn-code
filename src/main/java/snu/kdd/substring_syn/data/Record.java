package snu.kdd.substring_syn.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.substring_syn.utils.Util;

public class Record implements Comparable<Record> {
	
	public static final Record EMPTY_RECORD = new Record(new int[0]);
	public static TokenIndex tokenIndex = null;

	private int[] tokens;
	private final int id;
	private final int num_dist_tokens;

	public Rule[][] applicableRules = null;
	protected int[][] transformLengths = null;

	private long[] estTrans;

	private boolean validHashValue = false;
	private int hashValue;

	protected Rule[][] suffixApplicableRules = null;

	public int getQGramCount = 0;

	public Record( int id, String str, TokenIndex tokenIndex ) {
		this.id = id;
		String[] pstr = str.split( "( |\t)+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getIDOrAdd( pstr[ i ] );
		}
		
		num_dist_tokens = new IntOpenHashSet( tokens ).size();
	}

	public Record( int[] tokens ) {
		this.id = -1;
		this.tokens = tokens;
		if (tokens != null ) num_dist_tokens = new IntOpenHashSet( tokens ).size();
		else num_dist_tokens = 0;
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

	public int[] getTokensArray() {
		return tokens;
	}

	public int getTokenCount() {
		return tokens.length;
	}

	public int getDistinctTokenCount() {
		return num_dist_tokens;
	}

	/**
	 * Set applicable rules
	 */

	public void preprocessApplicableRules( ACAutomataR automata ) {
		applicableRules = automata.applicableRules( tokens );
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

	private void expandAll( ArrayList<Record> rslt, int idx, int[] t ) {

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

	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		if( !validHashValue ) {
			long tmp = 0;
			for( int token : tokens ) {
                tmp = ( tmp << 32 ) + token;
//                tmp = 0x1f1f1f1f ^ tmp + token;
				tmp = tmp % Util.bigprime;
			}
			hashValue = (int) ( tmp % Integer.MAX_VALUE );
			validHashValue = true;
		}
		return hashValue;
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
	
	public int getMinLength() {
		return transformLengths[ transformLengths.length - 1 ][ 0 ];
	}

	public int getMaxLength() {
		return transformLengths[ transformLengths.length - 1 ][ 1 ];
	}

	public int size() {
		return tokens.length;
	}

	public Collection<Integer> getTokens() {
		List<Integer> list = new ArrayList<Integer>();
		for( int i : tokens )
			list.add( i );
		return list;
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

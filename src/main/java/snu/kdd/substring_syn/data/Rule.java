package snu.kdd.substring_syn.data;

import java.util.Arrays;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public class Rule implements Comparable<Rule> {

//	private static int count = 0;
	public static ACAutomataR automata;
	public static final Rule[] EMPTY_RULE = new Rule[ 0 ];
	
	private final int idx;
	private final int[] lhs;
	private final int[] rhs;
//	private final int id;
	public final boolean isSelfRule;

	private final int hash;

	public static int[] getTokenIndexArray( String[] tokenArr ) {
		int[] indexArr = new int[tokenArr.length];
		for ( int i=0; i<tokenArr.length; ++i ) {
			indexArr[i] = Record.tokenIndex.getIDOrAdd(tokenArr[i]);
		}
		return indexArr;
	}

	protected Rule( int idx, int[] lhs, int[] rhs ) {
		this.idx = idx;
		this.lhs = lhs;
		this.rhs = rhs;
		this.hash = computeHash();
		this.isSelfRule = Arrays.equals(lhs, rhs);
//		id = count++;
	}

	private int computeHash() {
		int hash = 0;
		for( int i = 0; i < lhs.length; ++i ) hash = 0x1f1f1f1f ^ hash + lhs[ i ];
		for( int i = 0; i < rhs.length; ++i ) hash = 0x1f1f1f1f ^ hash + rhs[ i ];
		return hash;
	}
	
	public final int getID() {
		return idx;
	}

	public int[] getLhs() {
		return lhs;
	}

	public int[] getRhs() {
		return rhs;
	}

	public int lhsSize() {
		return lhs.length;
	}

	public int rhsSize() {
		return rhs.length;
	}

	@Override
	public int compareTo( Rule o ) {
		int compLhs = Records.compare(lhs, o.lhs);
		if ( compLhs == 0 ) return Records.compare(rhs,  o.rhs);
		else return compLhs;
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		if( this == o ) {
			return true;
		}
		Rule ro = (Rule) o;
		if( lhs.length == ro.lhs.length && rhs.length == ro.rhs.length ) {
			for( int i = 0; i < lhsSize(); ++i ) {
				if( lhs[ i ] != ro.lhs[ i ] ) {
					return false;
				}
			}
			for( int i = 0; i < rhsSize(); ++i ) {
				if( rhs[ i ] != ro.rhs[ i ] ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return Arrays.toString( lhs ) + " -> " + Arrays.toString( rhs );
	}

	public String toOriginalString() {
		StringBuilder bld = new StringBuilder();
		for( int i = 0; i < lhs.length; i++ ) {
			bld.append( Record.tokenIndex.getToken( lhs[ i ] ) + " " );
		}

		bld.append( "-> " );

		for( int i = 0; i < rhs.length; i++ ) {
			bld.append( Record.tokenIndex.getToken( rhs[ i ] ) + " " );
		}

		return bld.toString();
	}
}

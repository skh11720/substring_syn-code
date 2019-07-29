package snu.kdd.substring_syn.data;

import java.util.Arrays;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public class Rule implements Comparable<Rule> {
	final int[] lhs;
	final int[] rhs;
	public final int id;
	public final boolean isSelfRule;

	private final int hash;

	private static int count = 0;

	public static final Rule[] EMPTY_RULE = new Rule[ 0 ];
	
	public static Rule createRule( String str, TokenIndex tokenIndex ) {
		String[] rstr = str.split(", ");
		String[] lhsStr = rstr[0].trim().split(" ");
		String[] rhsStr = rstr[1].trim().split(" ");
		int[] lhs = getTokenIndexArray(lhsStr, tokenIndex);
		int[] rhs = getTokenIndexArray(rhsStr, tokenIndex);
		return new Rule(lhs, rhs);
	}
	
	public static Rule createRule( int from, int to ) {
		int[] lhs = new int[] {from};
		int[] rhs = new int[] {to};
		return new Rule(lhs, rhs);
	}
	
	private static int[] getTokenIndexArray( String[] tokenArr, TokenIndex tokenIndex ) {
		int[] indexArr = new int[tokenArr.length];
		for ( int i=0; i<tokenArr.length; ++i ) {
			indexArr[i] = tokenIndex.getIDOrAdd(tokenArr[i]);
		}
		return indexArr;
	}

	public Rule( int[] lhs, int[] rhs ) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.hash = computeHash();
		this.isSelfRule = Arrays.equals(lhs, rhs);
		id = count++;
	}

	private int computeHash() {
		int hash = 0;
		for( int i = 0; i < lhs.length; ++i ) hash = 0x1f1f1f1f ^ hash + lhs[ i ];
		for( int i = 0; i < rhs.length; ++i ) hash = 0x1f1f1f1f ^ hash + rhs[ i ];
		return hash;
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
		// only compares lhs
		return Records.compare( lhs, o.lhs );
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

	public void reindex( TokenOrder order ) {
		for ( int i=0; i<lhs.length; ++i ) {
			lhs[i] = order.getOrder(lhs[i]);
		}
		for ( int i=0; i<rhs.length; ++i ) {
			rhs[i] = order.getOrder(rhs[i]);
		}
	}
}

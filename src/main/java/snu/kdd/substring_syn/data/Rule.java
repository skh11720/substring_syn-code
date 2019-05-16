package snu.kdd.substring_syn.data;

import java.util.Arrays;

public class Rule implements Comparable<Rule> {
	int[] lhs;
	int[] rhs;
	public final int id;

	private final int hash;
	boolean isSelfRule = false;

	private static int count = 0;

	protected static final Rule[] EMPTY_RULE = new Rule[ 0 ];

	public Rule( String str, TokenIndex tokenIndex ) {
		int hash = 0;
		String[] pstr = str.split( ", " );
		String[] fpstr = pstr[ 0 ].trim().split( " " );

		lhs = new int[ fpstr.length ];
		for( int i = 0; i < fpstr.length; ++i ) {
			lhs[ i ] = tokenIndex.getIDOrAdd( fpstr[ i ] );
			hash = 0x1f1f1f1f ^ hash + lhs[ i ];
		}

		String[] tpstr = pstr[ 1 ].trim().split( " " );

		rhs = new int[ tpstr.length ];
		for( int i = 0; i < tpstr.length; ++i ) {
			rhs[ i ] = tokenIndex.getIDOrAdd( tpstr[ i ] );
			hash = 0x1f1f1f1f ^ hash + rhs[ i ];
		}
		this.hash = hash;
		id = count++;
	}

	// needed?
	public Rule( int[] from, int[] to ) {
		int hash = 0;
		this.lhs = from;
		this.rhs = to;
		for( int i = 0; i < from.length; ++i )
			hash = 0x1f1f1f1f ^ hash + from[ i ];
		for( int i = 0; i < to.length; ++i )
			hash = 0x1f1f1f1f ^ hash + to[ i ];
		this.hash = hash;
		id = count++;
	}

	// mostly used for self rule
	public Rule( int from, int to ) {
		int hash = 0;
		this.lhs = new int[ 1 ];
		this.lhs[ 0 ] = from;
		hash = 0x1f1f1f1f ^ hash + this.lhs[ 0 ];
		this.rhs = new int[ 1 ];
		this.rhs[ 0 ] = to;
		hash = 0x1f1f1f1f ^ hash + this.rhs[ 0 ];
		this.hash = hash;
		id = count++;
	}

	public boolean isSelfRule() {
		return Arrays.equals(lhs,  rhs);
	}

	public int[] getLeft() {
		return lhs;
	}

	public int[] getRight() {
		return rhs;
	}

	public int leftSize() {
		return lhs.length;
	}

	public int rightSize() {
		return rhs.length;
	}

	@Override
	public int compareTo( Rule o ) {
		// only compares lhs
		return Record.compare( lhs, o.lhs );
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
			for( int i = 0; i < leftSize(); ++i ) {
				if( lhs[ i ] != ro.lhs[ i ] ) {
					return false;
				}
			}
			for( int i = 0; i < rightSize(); ++i ) {
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

	public String toOriginalString( TokenIndex tokenIndex ) {
		StringBuilder bld = new StringBuilder();
		for( int i = 0; i < lhs.length; i++ ) {
			bld.append( tokenIndex.getToken( lhs[ i ] ) + " " );
		}

		bld.append( "-> " );

		for( int i = 0; i < rhs.length; i++ ) {
			bld.append( tokenIndex.getToken( rhs[ i ] ) + " " );
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

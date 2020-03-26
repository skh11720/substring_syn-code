package snu.kdd.substring_syn.data;

import snu.kdd.substring_syn.utils.Util;

public class Token {

	public final String str;
	
	public Token(String str) {
		this.str = str;
	}

	public Token(Substring str) {
		this.str = str.toString();
	}
	
	public final int length() {
		return str.length();
	}
	
	public final char charAt(int index) {
		return str.charAt(index);
	}
	
	@Override
	public int hashCode() {
		// djb2-like
		int hash = Util.bigprime;
		for ( int i=0; i<str.length(); ++i ) {
			hash = ( hash << 5 ) + Util.bigprime + str.charAt(i);
		}
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		if ( obj.getClass() == Token.class ) {
			Token o = (Token)obj;
			return str.equals(o.str);
		}
		else if ( obj.getClass() == Substring.class ) {
			Substring o = (Substring)obj;
			if ( length() != o.length() ) return false;
			for ( int i=0; i<length(); ++i ) {
				if ( charAt(i) != o.charAt(i) ) return false;
			}
			return true;
		}
		else return false;
	}
	
	@Override
	public String toString() {
		return str;
	}
}

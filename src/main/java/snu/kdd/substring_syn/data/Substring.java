package snu.kdd.substring_syn.data;

import java.nio.CharBuffer;
import java.util.stream.IntStream;

import snu.kdd.substring_syn.utils.Util;

public class Substring {

	private final char[] chseq;
	private final int begin;
	private final int end;
	
	public Substring(String str) {
		this(str.toCharArray(), 0, str.length());
	}
	
	public Substring(Substring str, int begin, int end) {
		this(str.chseq, str.begin+begin, str.begin+end);
	}

	public Substring(char[] chseq, int begin, int end) {
		this.chseq = chseq;
		this.begin = begin;
		this.end = end;
	}
	
	public final int length() {
		return end - begin;
	}
	
	public final char charAt(int index) {
		return chseq[begin+index];
	}

	public final int indexOf(int delim) {
		return indexOf(delim, 0);
	}

	public final int indexOf(int delim, int fromIndex) {
		for ( int i=fromIndex; i<length(); ++i ) {
			if ( chseq[begin+i] == delim ) return i;
		}
		return -1;
	}
	
	public final IntStream chars() {
		return CharBuffer.wrap(chseq, begin, length()).chars();
	}
	
	public final Substring substring(int begin, int end) {
		return new Substring(chseq, this.begin+begin, this.begin+end);
	}
	
	public final char[] toArray() {
		return chseq;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) return false;
		if ( obj.getClass() == Substring.class ) {
			Substring o = (Substring)obj;
			if ( length() != o.length() ) return false;
			for ( int i=0; i<length(); ++i ) {
				if ( this.chseq[begin+i] != o.chseq[o.begin+i] ) return false;
			}
			return true;
		}
		else if ( obj.getClass() == Token.class ) {
			Token o = (Token)obj;
			if ( length() != o.length() ) return false;
			for ( int i=0; i<length(); ++i ) {
				if ( this.chseq[begin+i] != o.charAt(i) ) return false;
			}
			return true;
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		// djb2-like
		int hash = Util.bigprime;
		for ( int i=begin; i<end; ++i ) {
			hash = ( hash << 5 ) + Util.bigprime + chseq[i];
		}
		return hash;
	}
	
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		for ( int i=begin; i<end; ++i ) bld.append(chseq[i]);
		return bld.toString();
	}
}

package snu.kdd.substring_syn.utils;

import java.util.Iterator;

import snu.kdd.substring_syn.data.Substring;

public class StringSplitIterator implements Iterator<Substring> {
	
	private final Substring str;
	private final char delim;
	private int begin;
	private int end;
	
	public StringSplitIterator(Substring str) {
		this(str, 0, ' ');
	}

	public StringSplitIterator(Substring str, char delim) {
		this(str, 0, delim);
	}

	public StringSplitIterator(Substring str, int beginidx, char delim) {
		this.delim = delim;
		this.str = str;
		end = beginidx - 1;
	}

	@Override
	public boolean hasNext() {
		return end < str.length();
	}

	@Override
	public Substring next() {
		begin = end + 1;
		end = str.indexOf(delim, begin);
		if ( end == -1 ) end = str.length();
		return new Substring(str, begin, end);
	}

}

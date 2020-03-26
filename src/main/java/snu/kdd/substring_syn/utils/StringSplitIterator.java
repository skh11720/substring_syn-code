package snu.kdd.substring_syn.utils;

import java.util.Iterator;

public class StringSplitIterator implements Iterator<String> {
	
	private final String str;
	private final char delim;
	private int begin;
	private int end;
	
	public StringSplitIterator(String str) {
		this(str, 0, ' ');
	}

	public StringSplitIterator(String str, char delim) {
		this(str, 0, delim);
	}

	public StringSplitIterator(String str, int beginidx, char delim) {
		this.delim = delim;
		this.str = str;
		end = beginidx - 1;
	}

	@Override
	public boolean hasNext() {
		return end < str.length();
	}

	@Override
	public String next() {
		begin = end + 1;
		end = str.indexOf(delim, begin);
		if ( end == -1 ) end = str.length();
		return str.substring(begin, end);
	}

}

package snu.kdd.substring_syn.utils.window;

import java.util.Iterator;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

public abstract class AbstractSlidingWindowIterator implements Iterator<IntList> {
	protected final int[] seq;
	protected final IntArrayList list;
	protected final int w;
	protected final int lenPrefix;
	protected int widx = -1;
	
	public AbstractSlidingWindowIterator( int[] seq, int w, double theta ) {
		this.seq = seq;
		this.list = IntArrayList.wrap(seq);
		this.w  = w;
		this.lenPrefix = w - (int)(Math.ceil(w*theta)) + 1;
	}
	
	public abstract IntSet getPrefix();

	protected void slide() {
		++widx;
	}

	public IntList getWindow() {
		return list.subList(widx, widx+w);
	}
	
	@Override
	public boolean hasNext() {
		return widx < seq.length-w;
	}
	
	@Override
	public IntList next() {
		if ( hasNext() ) {
			slide();
			return getWindow();
		}
		else throw new NoSuchElementException();
	}
}

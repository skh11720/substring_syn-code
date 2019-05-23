package snu.kdd.substring_syn.utils.window;

import java.util.Iterator;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;

public abstract class AbstractRecordSlidingWindow implements Iterator<Subrecord> {
	protected final Record rec;
	protected final int w;
	protected final int lenPrefix;
	protected int widx = -1;
	
	public AbstractRecordSlidingWindow( Record rec, int w, double theta ) {
		this.rec = rec;
		this.w  = w;
		this.lenPrefix = w - (int)(Math.ceil(w*theta)) + 1;
	}
	
	public abstract IntSet getPrefix();

	@Override
	public boolean hasNext() {
		return widx < rec.size()-w;
	}
	
	@Override
	public Subrecord next() {
		if ( hasNext() ) {
			slide();
			return getWindow();
		}
		else throw new NoSuchElementException();
	}

	protected void slide() {
		++widx;
	}
	
	protected Subrecord getWindow() {
		return new Subrecord(rec, widx, widx+w);
	}
}

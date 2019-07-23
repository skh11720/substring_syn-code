package snu.kdd.substring_syn.utils.window.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Subrecord;

public abstract class AbstractRecordSlidingWindowIterator implements Iterator<Subrecord> {
	protected final RecordInterface rec;
	protected final int w;
	protected final int lenPrefix;
	protected int widx = -1;
	
	public AbstractRecordSlidingWindowIterator( RecordInterface rec, int w, double theta ) {
		this.rec = rec;
		this.w  = w;
		this.lenPrefix = w - (int)(Math.ceil(w*theta)) + 1;
		
		if ( w > rec.size() ) 
			throw new IllegalArgumentException( String.format("window size w=%d must be smaller than or equal to rec.size=%d.", w, rec.size()) );
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

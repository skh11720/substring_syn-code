package snu.kdd.substring_syn.utils.window;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;

public class SortedWindowExpander implements Iterator<Subrecord> {

	final IntSet tokenSet;
	final IntSet prefix;
	final IntList list;
	final RecordInterface rec;
	final int widx;
	final double theta;
	int w = 0;
	
	public SortedWindowExpander( RecordInterface rec, int widx, double theta ) {
		tokenSet = new IntOpenHashSet();
		prefix = new IntOpenHashSet();
		list = new IntArrayList();
		this.rec = rec;
		this.widx = widx;
		this.theta = theta;
	}
	
	private int getPrefixLen() {
		return w - (int)(Math.ceil(w*theta)) + 1;
	}

	private void insert( int key ) {
		int idx = binarySearch(key);
		if ( idx < 0 ) idx = -idx-1;
		list.add(idx, key);
		tokenSet.add(key);
	}

	private int binarySearch( int key ) {
		int l = 0; 
		int r = list.size();
		while ( l < r ) {
			int m = (l+r)/2;
			int mKey = list.getInt(m);
			if ( key < mKey ) r = m;
			else if ( key > mKey ) l = m+1;
			else return m;
		}
		return -l-1;
	}

	public IntList getPrefix() {
		int prefixLen = getPrefixLen();
//		System.out.println("w: "+w);
//		System.out.println("prefixLen: "+prefixLen);
//		System.out.println("list.size: "+list.size());
		return list.subList(0, Math.min(prefixLen, list.size()));
	}
	
	public int getSetSize() {
		return tokenSet.size();
	}
	
//	public int getSize() {
//		return w;
//	}
	
	@Override
	public boolean hasNext() {
		return widx+w < rec.size();
	}

	@Override
	public Subrecord next() {
		int key = rec.getToken(widx+w);
		if ( !tokenSet.contains(key) ) insert(key);
		++w;
		return new Subrecord(rec, widx, widx+w);
	}
}

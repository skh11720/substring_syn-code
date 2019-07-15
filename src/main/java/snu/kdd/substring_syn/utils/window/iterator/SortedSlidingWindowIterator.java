package snu.kdd.substring_syn.utils.window.iterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SortedSlidingWindowIterator extends AbstractSlidingWindowIterator {
	
	final ObjectArrayList<Element> list;
	final Int2ObjectMap<Element> pos2elemMap;
	final IntSet prefix;
	
	public SortedSlidingWindowIterator( int[] seq, int w, double theta ) {
		super(seq, w, theta);
		list = new ObjectArrayList<>();
		pos2elemMap = new Int2ObjectOpenHashMap<>();
		prefix = new IntOpenHashSet(lenPrefix);
		for ( int i=0; i<w-1; ++i ) {
			updateList(i);
		}
	}
	
	private void updateList( int i ) {
		if ( list.size() >= w ) removeLastPos(i-w); 
		insert(i);
	}
	
	private void insert( int pos ) {
		int key = seq[pos];
		Element elem = new Element(key, pos);
		int idx = binarySearch(key);
		if ( idx < 0 ) idx = -idx-1;
		list.add(idx, elem);
		pos2elemMap.put(pos, elem);
		if (idx < lenPrefix) updatePrefix();
	}
	
	private void removeLastPos( int pos ) {
		Element elem = pos2elemMap.get(pos);
		int idx = binarySearch(elem.key);
		list.remove(idx);
		pos2elemMap.remove(pos);
		if (idx < lenPrefix) updatePrefix();
	}
	
	private int binarySearch( int key ) {
		int l = 0; 
		int r = list.size();
		while ( l < r ) {
			int m = (l+r)/2;
			int mKey = list.get(m).key;
			if ( key < mKey ) r = m;
			else if ( key > mKey ) l = m+1;
			else return m;
		}
		return -l-1;
	}
	
	private void updatePrefix() {
		prefix.clear();
		for ( int i=0; i<lenPrefix && i<list.size(); ++i ) prefix.add(list.get(i).key);
	}

	@Override
	protected void slide() {
		super.slide();
		updateList(widx+w-1);
	}

	@Override
	public IntSet getPrefix() {
		return prefix;
	}
	
	class Element {
		final int key;
		final int pos;
		
		public Element( int key, int pos ) {
			this.key = key;
			this.pos = pos;
		}
		
		@Override
		public String toString() {
			return String.format("(%d,%d)", key, pos);
		}
	}
}

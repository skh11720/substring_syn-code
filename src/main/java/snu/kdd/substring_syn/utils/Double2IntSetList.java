package snu.kdd.substring_syn.utils;

import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Double2IntSetList {
	
	/*
	 * This is an implementation of a sorted list of integer sets with
	 * the details as follows:
	 *   - each entry (integer set) is associated with a double as the score,
	 *   - an entry containes all items (integer) whose score is the score of the entry,
	 *   - the entries are sorted in decreasing order of the scores,
	 *   - it provides item-to-entry map and score-to-entry map,
	 *   - it supports increase-key operation for items called "update".
	 * 
	 * Compared with a normal list of (item, score) pairs,
	 * this is more efficient when many items share the same score.
	 */

	class Entry {
		double val;
		IntSet set;
		
		public Entry( double val ) {
			this.val = val;
			set = new IntOpenHashSet();
		}
		
		@Override
		public String toString() {
			return String.format("<%.3f:%s>", val, set);
		}
	}

	List<Entry> list; // sorted by val, decreasing order
	Int2ObjectMap<Entry> k2eMap;
	Double2ObjectMap<Entry> v2eMap;
	int size = 0;
	
	public Double2IntSetList() {
		list = new ObjectArrayList<>();
		k2eMap = new Int2ObjectOpenHashMap<>();
		v2eMap = new Double2ObjectOpenHashMap<>();
	}
	
	public int length() {
		return list.size();
	}
	
	public double getValue( int i ) {
		return list.get(i).val;
	}
	
	public IntSet getIntSet( int i ) {
		return list.get(i).set;
	}
	
	public void update( int k, double dv ) {
		double v = dv;
		if ( k2eMap.containsKey(k) ) {
			double vOld = removeKeyAndReturnOldValue(k);
			v += vOld;
		}
		insertNewKeyValue(k, v);
	}
	
	private void insertNewKeyValue( int k, double v ) {
		++size;
		Entry e = null;
		if ( v2eMap.containsKey(v) ) e = v2eMap.get(v);
		else {
			e = new Entry(v);
			insertNewEntry(e);
		}
		e.set.add(k);
		k2eMap.put(k, e);
	}
	
	private void insertNewEntry( Entry e ) {
		int l = 0;
		int r = list.size();
		while ( l < r ) {
			int m = (l+r)/2;
			double mv = list.get(m).val;
			if ( mv < e.val ) r = m;
			else l = m+1;
		}
		list.add(l, e);
		v2eMap.put(e.val, e);
	}
	
	private double removeKeyAndReturnOldValue( int k ) {
		--size;
		Entry e = k2eMap.get(k);
		e.set.remove(k);
		return e.val;
	}
	
	public String toString() {
		StringBuilder strbld = new StringBuilder();
		strbld.append('[');
		for ( Entry entry : list ) strbld.append(entry+", ");
		strbld.append(']');
		return strbld.toString();
	}
	
	public static void main(String[] args) {
		Random rn = new Random();
		int nRepeat = 10;
		int n = 20;
		
		for ( int repeat=0; repeat<nRepeat; ++repeat ) {
			Double2IntSetList list = new Double2IntSetList();
			for ( int i=0; i<n; ++i ) {
				int num = rn.nextInt(2)+1;
				int den = rn.nextInt(4)+1;
				int k = rn.nextInt(10);
				double v = 1.0*num/den;
				if ( list.k2eMap.containsKey(k) ) System.out.println("update ("+k+", v+"+String.format("%.3f", v)+")");
				else System.out.println("insert ("+k+", "+String.format("%.3f", v)+")");
				list.update(k, v);
				System.out.println(list);
			}
		}
	}
}

package snu.kdd.substring_syn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Query;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.IntHashBasedBinaryHeap;
import snu.kdd.substring_syn.utils.Util;

@FixMethodOrder
public class PrefixOfSlidingWindowTest {

	double[] thetaList = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
	int w = 7;
	
	static Query query;
	static double theta = 0.6;
	
	@BeforeClass
	public static void init() throws IOException {
		query = Util.getQuery("SPROT", 100000);
		TokenOrder order = new TokenOrder(query);
		query.reindexByOrder(order);
	}
	

	@Test
	public void testCorrectness() throws IOException {
		List<int[]> concatList = getListOfConcatRecords(query.searchedSet.recordList(), 5);
		getPrefixOfSlidingWindow(concatList, w, theta);
	}
	
	@Test
	public void testSimpleSlidingWindowTime() {
		List<int[]> concatList = getListOfConcatRecords(query.searchedSet.recordList(), 5);
		long ts = System.nanoTime();
		for ( int[] arr : concatList ) {
			SimpleSlidingWindow window1 = new SimpleSlidingWindow(arr, w, theta);
			while ( window1.hasNext() ) {
				window1.next();
				IntList subList = new IntArrayList( window1.getWindow() );
				IntSet prefix0 = getPrefix(subList, theta);
			}
		}
		System.out.println("testSimpleSlidingWindowTime: "+(System.nanoTime()-ts)/1e6+" ms");
	}

	@Test
	public void testSortedSlidingWindowTime() {
		List<int[]> concatList = getListOfConcatRecords(query.searchedSet.recordList(), 5);
		long ts = System.nanoTime();
		for ( int[] arr : concatList ) {
			SortedSlidingWindow window1 = new SortedSlidingWindow(arr, w, theta);
			while ( window1.hasNext() ) {
				window1.next();
				IntList subList = new IntArrayList( window1.getWindow() );
				IntSet prefix0 = getPrefix(subList, theta);
			}
		}
		System.out.println("testSortedSlidingWindowTime: "+(System.nanoTime()-ts)/1e6+" ms");
	}

	

	public static IntOpenHashSet getPrefix( IntList tokenSet, double theta ) {
		int lenPrefix = tokenSet.size() - (int)(Math.ceil(tokenSet.size()*theta)) + 1;
		IntOpenHashSet prefix = new IntOpenHashSet( tokenSet.stream().sorted().limit(lenPrefix).iterator() );
		return prefix;
	}
	
	public static List<int[]> getListOfConcatRecords( List<Record> recordList, int numConcat ) {
		ObjectArrayList<int[]> concatList = new ObjectArrayList<>();
		IntArrayList concated = new IntArrayList();
		for ( int i=0; i<recordList.size(); ++i ) {
			Record rec = recordList.get(i);
			concated.addElements(concated.size(), rec.getTokensArray());
			if ( (i+1) % numConcat == 0 || i == recordList.size()-1 ) {
				concatList.add(concated.toIntArray());
				concated.clear();
			}
		}
		return concatList;
	}
	
	public static void getPrefixOfSlidingWindow( List<int[]> concatList, int w, double theta ) {
		for ( int[] arr : concatList ) {
//			System.out.println(Arrays.toString(arr));
			SimpleSlidingWindow window1 = new SimpleSlidingWindow(arr, w, theta);
//			HeapBasedSlidingWindow window2 = new HeapBasedSlidingWindow(arr, w, theta);
			SortedSlidingWindow window3 = new SortedSlidingWindow(arr, w, theta);
			while ( window1.hasNext() ) {
				window1.next();
//				window2.next();
				window3.next();
				IntList subList = new IntArrayList( window1.getWindow() );
				IntSet prefix0 = getPrefix(subList, theta);
				IntSet prefix1 = window1.getPrefix();
//				IntSet prefix2 = window2.getPrefix();
				IntSet prefix3 = window3.getPrefix();
//				System.out.println(prefix0.equals(window1.getPrefix()));
//				System.out.println(prefix0.equals(window2.getPrefix()));
//				System.out.println(subList);
//				System.out.println(prefix0+"\t"+prefix2);
				assertTrue(prefix0.equals(prefix3));
			}
//			break;
		}
	}
}

abstract class AbstractSlidingWindow implements Iterator<IntList> {
	final int[] seq;
	final IntArrayList list;
	final int w;
	final int lenPrefix;
	int widx = -1;
	
	public AbstractSlidingWindow( int[] seq, int w, double theta ) {
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

class SimpleSlidingWindow extends AbstractSlidingWindow {

	public SimpleSlidingWindow( int[] seq, int w, double theta ) {
		super(seq, w, theta);
	}
	
	@Override
	public IntSet getPrefix() {
		return new IntOpenHashSet( getWindow().stream().sorted().limit(lenPrefix).iterator() );
	}
}

class HeapBasedSlidingWindow extends AbstractSlidingWindow {
	final IntHashBasedBinaryHeap heap;
	
	public HeapBasedSlidingWindow( int[] seq, int w, double theta ) {
		super(seq, w, theta);
		heap = new IntHashBasedBinaryHeap(lenPrefix+1, (x,y)->Integer.compare(y, x));
		for ( int i=0; i<w-1; ++i ) {
			updateHeap(i);
		}
		System.out.println("INIT: "+Arrays.toString(heap.getKeys()));
		System.out.println(heap.peek());
	}
	
	private void updateHeap( int i ) {
		if ( heap.size() == 0 || seq[i] < heap.peek() ) {
			if ( heap.size() >= lenPrefix ) heap.poll();
			heap.insert( seq[i], i );
		}
	}
	
	@Override
	protected void slide() {
		super.slide();
		if ( widx > 0 ) heap.deleteByValueIfExists(widx-1);
		if ( widx+w-1 < seq.length ) {
			System.out.println("Add "+(widx+w-1)+", "+seq[widx+w-1]);
			updateHeap(widx+w-1);
		}
	}
	
	@Override
	public IntSet getPrefix() {
		return new IntOpenHashSet( heap.getKeys() );
	}
}

class SortedSlidingWindow extends AbstractSlidingWindow {
	
	//TODO: build a list of (key, pos)
	final ObjectArrayList<Element> list;
	final Int2ObjectMap<Element> pos2elemMap;
	final IntSet prefix;
	
	public SortedSlidingWindow( int[] seq, int w, double theta ) {
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

package snu.kdd.substring_syn;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Query;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.IntHashBasedBinaryHeap;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.AbstractSlidingWindow;
import snu.kdd.substring_syn.utils.window.SimpleSlidingWindow;
import snu.kdd.substring_syn.utils.window.SortedSlidingWindow;

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

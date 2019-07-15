package snu.kdd.substring_syn;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.utils.IntHashBasedBinaryHeap;

public class IntHashBasedBinaryHeapTest {
	
	Random rn = new Random(0);
	Comparator<Integer> comp = (x,y) -> -Integer.compare(x, y);

	@Test
	public void insertCorrectnessTest() {
		IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap(comp);
		int n = 10000;
		IntArrayList valueList = new IntArrayList();
		
		for ( int i=0; i<n; ++i ) {
			int key = rn.nextInt();
			int value = i;
			valueList.add(value);
			heap.insert(key, value);
			assertTrue( heap.isValidHeap() );
//			System.out.println(String.format("%d/%d", heap.size(), heap.capacity()));
		}
	}
	
	@Test
	public void deleteCorrectnessTest() {
		IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap(comp);
		int n = 10000;
		IntArrayList valueList = new IntArrayList();
		
		for ( int i=0; i<n; ++i ) {
			int key = rn.nextInt();
			int value = i;
			valueList.add(value);
			heap.insert(key, value);
		}
		
		Collections.shuffle(valueList);
		for ( int value : valueList ) {
			heap.deleteByValue(value);
			assertTrue( heap.isValidHeap() );
//			System.out.println(String.format("%d/%d", heap.size(), heap.capacity()));
		}
	}

	@Test
	public void timeEfficiencyTest() {
		int n = 100000;
		IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap(comp);
		IntArrayList valueList = new IntArrayList();
		for ( int i=0; i<n; ++i ) {
			int key = rn.nextInt();
			int value = i;
			valueList.add(value);
			heap.insert(key, value);
		}
		
		int m = 1000000;
		long n_ins = 0;
		long t_ins = 0;
		long n_del = 0;
		long t_del = 0;
		for ( int i=0; i<m; ++i ) {
			if ( rn.nextDouble() < 0.51 ) { // insert
				++n_ins;
				int key = rn.nextInt();
				int value = n++;
				long ts = System.nanoTime();
				heap.insert(key, value);
				t_ins += System.nanoTime() - ts;
				valueList.add(value);
			}
			else { // delete
				++n_del;
				if ( heap.isEmpty() ) continue;
				int idx = rn.nextInt( valueList.size() );
				int value = valueList.getInt(idx);
				long ts = System.nanoTime();
				heap.deleteByValue(value);
				t_del += System.nanoTime() - ts;
				valueList.removeInt(idx);
			}
//			assertTrue( heap.isValidHeap() );
		}
		
		System.out.println("#Insert: "+n_ins);
		System.out.println("Avg Insert Time: "+(t_ins/1e6/n_ins) +" ms");
		System.out.println("#Delete: "+n_del);
		System.out.println("Avg Delete Time: "+(t_del/1e6/n_del) +" ms");
	}
}

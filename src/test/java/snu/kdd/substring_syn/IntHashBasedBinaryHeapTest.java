package snu.kdd.substring_syn;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.utils.IntHashBasedBinaryHeap;

public class IntHashBasedBinaryHeapTest {
	
	Random rn = new Random(0);

	@Test
	public void insertCorrectnessTest() {
		IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap();
		int n = 10000;
		IntArrayList keyList = new IntArrayList();
		
		for ( int i=0; i<n; ++i ) {
			int key = rn.nextInt();
			while ( heap.containesKey(key) ) key = rn.nextInt();
			keyList.add(key);
			heap.insert(key);
			assertTrue( heap.isValidHeap() );
//			System.out.println(String.format("%d/%d", heap.size(), heap.capacity()));
		}
	}
	
	@Test
	public void deleteCorrectnessTest() {
		IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap();
		int n = 10000;
		IntArrayList keyList = new IntArrayList();
		
		for ( int i=0; i<n; ++i ) {
			int key = rn.nextInt();
			while ( heap.containesKey(key) ) key = rn.nextInt();
			keyList.add(key);
			heap.insert(key);
		}
		
		Collections.shuffle(keyList);
		for ( int key : keyList ) {
			heap.delete(key);
			assertTrue( heap.isValidHeap() );
//			System.out.println(String.format("%d/%d", heap.size(), heap.capacity()));
		}
	}

	@Test
	public void timeEfficiencyTest() {
		int n = 100000;
		IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap();
		IntArrayList keyList = new IntArrayList();
		for ( int i=0; i<n; ++i ) {
			int key = rn.nextInt();
			while ( heap.containesKey(key) ) key = rn.nextInt();
			keyList.add(key);
			heap.insert(key);
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
				while ( heap.containesKey(key) ) key = rn.nextInt();
				long ts = System.nanoTime();
				heap.insert(key);
				t_ins += System.nanoTime() - ts;
				keyList.add(key);
			}
			else { // delete
				++n_del;
				if ( heap.isEmpty() ) continue;
				int idx = rn.nextInt( keyList.size() );
				int key = keyList.getInt(idx);
				long ts = System.nanoTime();
				heap.delete(key);
				t_del += System.nanoTime() - ts;
				keyList.removeInt(idx);
			}
			assertTrue( heap.isValidHeap() );
		}
		
		System.out.println("#Insert: "+n_ins);
		System.out.println("Avg Insert Time: "+(t_ins/1e6/n_ins));
		System.out.println("#Delete: "+n_del);
		System.out.println("Avg Delete Time: "+(t_del/1e6/n_del));
	}
}

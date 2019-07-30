package snu.kdd.deprecated;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.blumarsh.graphmaker.core.util.FibonacciHeap;
import com.blumarsh.graphmaker.core.util.FibonacciHeap.Node;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.utils.IntHashBasedBinaryHeap;

@Ignore
public class HeapPerformanceTest {
	
	private static PrintStream ps = null;
	
	
	@BeforeClass
	public static void setup() throws FileNotFoundException {
		ps = new PrintStream( new FileOutputStream("tmp/HeapTest.log"));
	}
	
	@AfterClass
	public static void cleanup() {
		ps.flush();
		ps.close();
	}
	
	@Test
	public void testPriorityQueueDelete() throws IOException {
		ps.println( new Object(){}.getClass().getEnclosingMethod().getName() );
		final Random rn = new Random(0);
		final int nRepeat = 5;
		final int m = 100;
		
		for ( int n=1000; n<1e7; n*=2 ) {
			double t_avg = 0;
			for ( int repeat=0; repeat<nRepeat; ++repeat ) {
				int[] elems = rn.ints(n).toArray();
				IntArrayList elemList = IntArrayList.wrap(elems);
				
				PriorityQueue<Integer> heap = new PriorityQueue<>();
				for ( int i=0; i<n; ++i ) heap.add(elems[i]);
				Collections.shuffle(elemList);

				long ts = System.nanoTime();
				for ( int j=0; j<m; ++j ) {
					heap.remove(elemList.getInt(j));
				}
				long t = System.nanoTime() - ts;
				t_avg += t;
			}
			t_avg /= nRepeat;
			ps.println(String.format("%10d\t%.6f", n, t_avg/1e6));
		}
	}

	@Test
	public void testFibonacciHeapDelete() throws IOException {
		ps.println( new Object(){}.getClass().getEnclosingMethod().getName() );
		final Random rn = new Random(0);
		final int nRepeat = 5;
		final int m = 100;
		
		for ( int n=1000; n<1e7; n*=2 ) {
			double t_avg = 0;
			for ( int repeat=0; repeat<nRepeat; ++repeat ) {
				int[] elems = rn.ints(n).toArray();
				List<Node> nodeList = new ObjectArrayList<>();
				
				FibonacciHeap fibheap = new FibonacciHeap();
				for ( int i=0; i<n; ++i ) nodeList.add( fibheap.insert(elems[i], elems[i]) );
				Collections.shuffle(nodeList);

				long ts = System.nanoTime();
				for ( int j=0; j<m; ++j ) {
					fibheap.delete(nodeList.get(j));
				}
				long t = System.nanoTime() - ts;
				t_avg += t;
			}
			t_avg /= nRepeat;
			ps.println(String.format("%10d\t%.6f", n, t_avg/1e6));
		}
	}

	@Test
	public void testIntHashBasedBinaryHeapDelete() throws IOException {
		ps.println( new Object(){}.getClass().getEnclosingMethod().getName() );
		final Random rn = new Random(0);
		final int nRepeat = 5;
		final int m = 100;
		
		for ( int n=1000; n<1e7; n*=2 ) {
			double t_avg = 0;
			for ( int repeat=0; repeat<nRepeat; ++repeat ) {
				int[] keys = rn.ints(n).toArray();
				int[] values = rn.ints(n).toArray();
				IntArrayList valueList = IntArrayList.wrap(values);
				
				IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap();
				for ( int i=0; i<n; ++i ) heap.insert(keys[i], values[i]);
				Collections.shuffle(valueList);

				long ts = System.nanoTime();
				for ( int j=0; j<m; ++j ) {
					heap.deleteByValue(valueList.getInt(j));
				}
				long t = System.nanoTime() - ts;
				t_avg += t;
			}
			t_avg /= nRepeat;
			ps.println(String.format("%10d\t%.6f", n, t_avg/1e6));
		}
	}
}

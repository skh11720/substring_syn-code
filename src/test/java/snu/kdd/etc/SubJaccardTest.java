package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import snu.kdd.substring_syn.utils.Util;

public class SubJaccardTest {
	
	Random rn = new Random(0);
	int m = 10;
	int lmin = 1;
	int lmax = m;


	@Test
	public void testSubJaccard() {
		int nRepeat = 5;
		for ( int repeat=0; repeat<nRepeat; ++repeat ) {
			int[] x = genRandomArr();
			int[] y = genRandomArr();
			System.out.println(Arrays.toString(x));
			System.out.println(Arrays.toString(y));
			System.out.println(subJaccard1(x, y));
		}
	}

	@Test
	public void compareSubjaccard() {
		long[] tArr = new long[4];
		
		for ( int l=0; l<1000; ++l ) {
			int lx = rn.nextInt(5)+3;
			int ly = rn.nextInt(100)+5;
			int[] x = new int[lx];
			int[] y = new int[ly];
			for ( int i=0; i<lx; ++i ) x[i] = rn.nextInt(10);
			for ( int i=0; i<ly; ++i ) y[i] = rn.nextInt(10);
			IntList xList = IntArrayList.wrap(x);
			IntList yList = IntArrayList.wrap(y);
			
			long ts = System.nanoTime();
			double sim0 = Util.subJaccard0(x, y);
			tArr[0] += System.nanoTime() - ts;
			ts = System.nanoTime();
			double sim1 = Util.subJaccard1(xList, yList);
			tArr[1] += System.nanoTime() - ts;
			ts = System.nanoTime();
			double sim2 = Util.subJaccard(xList, yList);
			tArr[2] += System.nanoTime() - ts;
			ts = System.nanoTime();
			double sim3 = Util.subJaccardM(xList, yList);
			tArr[3] += System.nanoTime() - ts;
			assertTrue(Math.abs(sim0 - sim1) < 1e-7);
			assertTrue(Math.abs(sim0 - sim2) < 1e-7);
		}
		
		for (int i=0; i<tArr.length; ++i ) {
			System.out.println(tArr[i]/1.0e6);
		}
	}
	
	@Test
	public void testSubJaccardM() {
		int nRepeat = 10;
		for ( int repeat=0; repeat<nRepeat; ++repeat ) {
			int[] x = genRandomArr();
			int[] y = genRandomArr();
			System.out.println(Arrays.toString(x));
			System.out.println(Arrays.toString(y));
			System.out.println(Util.subJaccardM(x, y));
		}
	}

	
	int[] genRandomArr() {
		int l = (int)(rn.nextDouble()*(lmax+1-lmin))+lmin;
		int[] a = new int[l];
		for ( int i=0; i<l; ++i ) a[i] = rn.nextInt(m);
		return a;
	}
	
	double subJaccard1( int[] x, int[] y ) {
		double simMax = 0;
		for ( int i=0; i<y.length; ++i ) {
			for ( int j=i; j<y.length; ++j ) {
				double sim = Util.jaccard(x, Arrays.copyOfRange(y, i, j+1));
				simMax = Math.max(simMax, sim);
			}
		}
		return simMax;
	}
}

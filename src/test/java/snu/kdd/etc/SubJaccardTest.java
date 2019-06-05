package snu.kdd.etc;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import snu.kdd.substring_syn.utils.Util;

public class SubJaccardTest {
	
	Random rn = new Random();
	int m = 10;
	int lmin = 1;
	int lmax = m;


	@Test
	public void test() {
		int nRepeat = 5;
		for ( int repeat=0; repeat<nRepeat; ++repeat ) {
			int[] x = genRandomArr();
			int[] y = genRandomArr();
			System.out.println(Arrays.toString(x));
			System.out.println(Arrays.toString(y));
			System.out.println(subJaccard1(x, y));
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

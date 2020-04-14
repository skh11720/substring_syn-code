package snu.kdd.etc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.xerial.snappy.Snappy;

public class SnappyTest {
	/*
	 * Calling Snappy.rawCompress with insufficient-sized buffer MAY CORRUPT data after that buffer.
	 */

	@Test
	public void testRawCompress() throws IOException {
		Random rn = new Random(0);
		int n = 10;
		int[] ibuf = rn.ints(-100, 100).limit(n).toArray();
		int[] ibufNew = new int[100000];
		byte[] bbuf = new byte[0];
		int maxCompLen = Snappy.maxCompressedLength(ibuf.length*Integer.BYTES);
		System.out.println("input length: "+ibuf.length*Integer.BYTES);
		System.out.println("maxCompressedLength: "+maxCompLen);
		int blen = Snappy.rawCompress(ibuf, 0, ibuf.length*Integer.BYTES, bbuf, 0);
		System.out.println("compressedLength: "+blen);

		byte[] bbuf0 = new byte[blen/2];
		byte[] bbuf1 = new byte[blen];
		byte[] bbuf2 = new byte[blen*2];

		int blen1 = Snappy.rawCompress(ibuf, 0, ibuf.length*Integer.BYTES, bbuf1, 0);
		int blen0 = Snappy.rawCompress(ibuf, 0, ibuf.length*Integer.BYTES, bbuf0, 0);
		int blen2 = Snappy.rawCompress(ibuf, 0, ibuf.length*Integer.BYTES, bbuf2, 0);
//		
		System.out.println("blen0: "+blen0);
//		System.out.println("blen1: "+blen1);
//		System.out.println("blen2: "+blen2);
		
		int bolen = Snappy.rawUncompress(bbuf0, 0, blen0, ibufNew, 0);
		System.out.println("bolen: "+bolen);
	}

	@Ignore
	public void testEfficiency() throws IOException {
		/*
		       n         ts0         ts1
			  10     298.993      28.588
			 100     302.598      76.571
			1000     306.022     188.461
		   10000     314.140     644.289

		 */
		long ts0 = 0;
		long ts1 = 0;
		Random rn = new Random(0);
		byte[] bbuf = new byte[100000];
		int nTries=100000;
		
		
		System.out.printf("%8s%12s%12s\n", "n", "ts0", "ts1");
		
		for ( int n : new int[] {10, 100, 1000, 10000} ) {
			for ( int tries=0; tries<nTries; ++tries ) {
				int[] ibuf = rn.ints().limit(n).toArray();
				long ts = System.nanoTime();
				Snappy.maxCompressedLength(ibuf.length*Integer.BYTES);
				ts0 += System.nanoTime() - ts;
				ts = System.nanoTime();
				Snappy.rawCompress(ibuf,  0, ibuf.length*Integer.BYTES, bbuf, 0);
				ts1 += System.nanoTime() - ts;
			}
			
			System.out.printf("%8d", n);
			System.out.printf("%12.3f", ts0/1e6);
			System.out.printf("%12.3f", ts1/1e6);
			System.out.println();
		}
	}
}

package snu.kdd.substring_syn.index;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.NaiveIndexStore;
import snu.kdd.substring_syn.algorithm.index.inmem.NaiveInvertedIndex;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.record.Record;

@SuppressWarnings("unused")
public class NaiveIndexStoreTest {

	@Test
	public void checkCorrectness() throws IOException {
		long t;
		Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "1000000");
		ObjectList<Record> recordList = new ObjectArrayList<Record>(dataset.getIndexedList().iterator());
		NaiveInvertedIndex index0 = new NaiveInvertedIndex(dataset);
		
		t = System.nanoTime();
		NaiveIndexStore store = new NaiveIndexStore(recordList);
		System.out.println("NaiveIndexStore construction: "+(System.nanoTime()-t)/1e6);
		
		for ( int token=0; token<500000; ++token ) {
			ObjectList<Record> recList = index0.getInvList(token);
			IntList invList0 = null;
			if ( recList != null ) invList0 = new IntArrayList(recList.stream().map(rec->rec.getID()).iterator());
			IntList invList1 = store.getInvList(token);
			
			try {
				if ( invList0 == null ) assertTrue(invList1 == null);
				else assertTrue( invList0.equals(invList1) );
			} catch ( AssertionError e ) {
				System.err.println("invList0 = "+invList0);
				System.err.println("invList1 = "+invList1);
				System.exit(1);
			}
			
			ObjectList<Record> trecList = index0.getTransInvList(token);
			IntList tinvList0 = null;
			if ( trecList != null ) tinvList0 = new IntArrayList(trecList.stream().map(rec->rec.getID()).iterator());
			IntList tinvList1 = store.getTrInvList(token);
			
			try {
				if ( tinvList0 == null ) assertTrue(tinvList1 == null);
				else assertTrue( tinvList0.equals(tinvList1) );
			} catch ( AssertionError e ) {
				System.err.println("tinvList0 = "+tinvList0);
				System.err.println("tinvList1 = "+tinvList1);
				System.exit(1);
			}
		}
	}

	@Test
	public void repeatedConstruction() throws IOException {
		int retries = 100;
		for ( int i=0; i<retries; ++i ) {
			Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "100000");
			NaiveIndexStore store = new NaiveIndexStore(dataset.getIndexedList());
		}
	}

	@Test
	public void efficiencyTestVaryingDataSize() throws IOException {
		String[] sizeArr = {"10000", "31622", "100000", "316227", "1000000"};
		long[] tArr = new long[sizeArr.length];
		long t;

		for ( int i=0; i<sizeArr.length; ++i ) {
			String size = sizeArr[i];
			Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", size);
			t = System.nanoTime();
			NaiveIndexStore store = new NaiveIndexStore(dataset.getIndexedList());
			tArr[i] = System.nanoTime()-t;
			System.out.println(String.format("%10s\t%f", sizeArr[i], tArr[i]/1e6));
		}
	}
	/*
	 *      10000	950.980500
	 *      31622	1807.851100
	 *      100000	5463.560700
	 *      316227	15483.032700
	 *      1000000	47982.548700
	 */

	@Test
	public void efficiencyTestVaryingMemSize() throws IOException {
		int[] cArr = {1, 2, 4, 8, 16, 32, 64, 128};
		long[] tArr = new long[cArr.length];
		long t;

		for ( int i=0; i<cArr.length; ++i ) {
			int c = cArr[i];
			Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "1000000");
			t = System.nanoTime();
			NaiveIndexStore store = new NaiveIndexStore(dataset.getIndexedList(), c*1024*1024);
			tArr[i] = System.nanoTime()-t;
			System.out.println(String.format("%10s\t%f", cArr[i], tArr[i]/1e6));
		}
	}
	/*
	 *         1	47623.250600
	 *         2	41033.337200
	 *         4	37290.372699
	 *         8	35358.299400
	 *         16	33241.377300
	 *         32	33759.941101 
	 *         64	39819.063900
	 *         128	35178.852200
	 */
}

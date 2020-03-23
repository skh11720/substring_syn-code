package snu.kdd.faerie;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSearchTest {
	
	@Test
	public void testFaerieIndex() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "10000", "3");
		FaerieMemBasedIndex index0 = new FaerieMemBasedIndex(dataset.getIndexedList());
		for ( int i=0; i<index0.store.size(); ++i ) index0.getEntry(i);
		FaerieDiskBasedIndex index1 = new FaerieDiskBasedIndex(dataset.getIndexedList());
		for ( int i=0; i<index1.store.size(); ++i ) index1.getEntry(i);
	}
	
	@Test
	public void testSingleRun() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "3162", "3", "1.0");
		double theta = 0.6;
		AbstractSearch alg1 = new FaerieSearch(theta, false);
		alg1.run(dataset);
	}

	@Test
	public void testCorrectness() throws IOException {

		String[] attrs = {
				Stat.Num_Result,
				Stat.Num_QS_Result,
				Stat.Num_TS_Result,
//				Stat.Num_QS_Verified,
//				Stat.Num_TS_Verified,
//				Stat.Len_QS_Verified,
//				Stat.Len_TS_Verified,
//				Stat.Len_QS_Retrieved,
//				Stat.Len_TS_Retrieved,
//				Stat.Len_QS_LF,
//				Stat.Len_TS_LF,
//				Stat.Len_QS_PF,
//				Stat.Len_TS_PF,
		};

		for ( Dataset dataset : TestDatasetManager.getAllDatasets() ) {
			for ( double theta : new double[] {0.6, 1.0} ) {
//				if ( theta != 0.6 || !dataset.name.equals("WIKI_n10000_r10000_q3") ) continue;
				AbstractSearch alg0 = new FaerieNaiveSearch(theta);
				alg0.run(dataset);
				AbstractSearch alg1 = new FaerieSearch(theta, true);
				alg1.run(dataset);
				
				for ( String attr : attrs ) {
					int val0 = Integer.parseInt(alg0.getStatContainer().getStat(attr));
					int val1 = Integer.parseInt(alg1.getStatContainer().getStat(attr));
					assertEquals(val0, val1);
//					System.out.print((val0 == val1)+"\t");
				}
//				System.out.println();
			}
		}
	}
	
	@Test
	public void testEfficiency() {
		/*
		 * 692.9975999999999
		 * binarySpan: 669.1880000000001
		 * binarySpan+Shift: 656.3233
		 */
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");
		double theta = 0.6;
		double t_sum = 0;
		int nTries = 10;
		for ( int i=0; i<nTries; ++i ) {
			AbstractSearch alg1 = new FaerieSearch(theta, true);
			alg1.run(dataset);
			t_sum += Double.parseDouble(alg1.getStatContainer().getStat(Stat.Time_Total));
		}
		System.out.println(t_sum/nTries);
	}
	
	@Test
	public void testSimilarityCalculator() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");
		for ( Record query : dataset.getSearchedList() ) {
			for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
				SimilarityCalculator simcal = new SimilarityCalculator(query);
				for ( int i=0; i<rec.size(); ++i ) {
					double sim0 = Util.jaccardM(query.getTokenList(), rec.getTokenList().subList(0, i+1));
					simcal.add(rec.getToken(i));
					double sim1 = simcal.compute();
					assertEquals(sim0, sim1, 1e-10);
				}
			}
		}
	}
	
	private class SimilarityCalculator {
		final Int2IntOpenHashMap counter;
		int num = 0, den = 0;

		public SimilarityCalculator( Record query ) {
			counter = new Int2IntOpenHashMap();
			for ( int token : query.getTokens() ) counter.addTo(token, 1);
			den = query.size();
		}
		
		public void add( int token ) {
			if ( counter.containsKey(token) && counter.get(token) > 0 ) {
				num += 1;
				counter.addTo(token, -1);
			}
			else den += 1;
		}
		
		public double compute() {
			return 1.0*num/den;
		}
	}

//	@Test
//	public void testDiskBased() throws IOException, ClassNotFoundException {
//		int[] arr = {3, 2, 5, 3, 7, 3, 2, 3, 1};
//		FaerieIndexEntry entry = new FaerieIndexEntry(arr); 
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(bos);
//		oos.writeObject(entry);
//		oos.flush();
//		byte[] buf = bos.toByteArray();
//		System.out.println(Arrays.toString(buf));
//		
//		ByteArrayInputStream bis = new ByteArrayInputStream(buf);
//		ObjectInputStream ois = new ObjectInputStream(bis);
//		FaerieIndexEntry entry2 = (FaerieIndexEntry)ois.readObject();
//		System.out.println(entry2);
//	}
}

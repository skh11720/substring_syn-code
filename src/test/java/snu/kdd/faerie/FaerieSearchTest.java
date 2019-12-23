package snu.kdd.faerie;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class FaerieSearchTest {

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
				AbstractSearch alg0 = new FaerieNaiveSearch(theta);
				alg0.run(dataset);
				AbstractSearch alg1 = new FaerieSearch(theta);
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
		 */
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");
		double theta = 0.6;
		double t_sum = 0;
		int nTries = 5;
		for ( int i=0; i<nTries; ++i ) {
			AbstractSearch alg1 = new FaerieSearch(theta);
			alg1.run(dataset);
			t_sum += Double.parseDouble(alg1.getStatContainer().getStat(Stat.Time_Total));
		}
		System.out.println(t_sum/nTries);
	}
	
	@Test
	public void testSimilarityCalculator() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");
		for ( Record query : dataset.getSearchedList() ) {
			for ( Record rec : dataset.getIndexedList() ) {
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
}

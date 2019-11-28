package snu.kdd.substring_syn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Stat;

public class PrefixSearchRefactorInvarianceTest {
	

	@Test
	public void test() {
		boolean onTest = true;

		String[] attrs = {
				Stat.Num_Result,
				Stat.Num_QS_Result,
				Stat.Num_TS_Result,
				Stat.Num_QS_Verified,
				Stat.Num_TS_Verified,
				Stat.Len_QS_Verified,
				Stat.Len_TS_Verified,
				Stat.Len_QS_Retrieved,
				Stat.Len_TS_Retrieved,
				Stat.Len_QS_LF,
				Stat.Len_TS_LF,
				Stat.Len_QS_PF,
				Stat.Len_TS_PF,
		};
		int[] values = {
				58, 49, 58, 17964598, 17430249, 195065984, 188874271, 1335465, 1299437, 0, 0, 195065984, 0, 
				58, 49, 58, 1064550, 975201, 13781570, 12584658, 65170, 60901, 0, 0, 13781570, 0, 
				58, 49, 58, 183021, 1308953, 2144078, 17011938, 1335465, 1299437, 0, 0, 2144078, 17011938, 
				58, 49, 58, 47004, 217449, 679288, 3319322, 65170, 60901, 0, 0, 679288, 3319322, 
				58, 49, 58, 7416406, 12373547, 44470312, 79136785, 1335465, 1299437, 44470312, 79136785, 44470312, 0, 
				58, 49, 58, 407153, 630640, 2582607, 4324520, 65170, 60901, 2582607, 4324520, 2582607, 0, 
				58, 49, 58, 71164, 917493, 402274, 7042831, 1335465, 1299437, 44470312, 53012363, 402274, 7042831, 
				58, 49, 58, 16652, 133900, 99268, 1072236, 65170, 60901, 2582607, 8192069, 99268, 1072236, 
				8, 8, 8, 17971381, 17449938, 195143118, 189132598, 1335465, 1299437, 0, 0, 195143118, 0, 
				8, 8, 8, 7713, 2900, 134572, 58076, 437, 274, 0, 0, 134572, 0, 
				8, 8, 8, 5996, 200980, 43210, 2744815, 1335465, 1299437, 0, 0, 43210, 2744815, 
				8, 8, 8, 156, 702, 1124, 19462, 437, 274, 0, 0, 1124, 19462, 
				8, 8, 8, 1879605, 8875780, 10525847, 39324003, 1335465, 1299437, 10525847, 39324003, 10525847, 0, 
				8, 8, 8, 417, 1178, 2201, 5495, 437, 274, 2201, 5495, 2201, 0, 
				8, 8, 8, 727, 100678, 4068, 521384, 1335465, 1299437, 10525847, 11129215, 4068, 521384, 
				8, 8, 8, 28, 137, 148, 805, 437, 274, 2201, 13535, 148, 805, 
		};
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");

		IntIterator iter = null;
		StringBuilder strbld = null;
		if (onTest) iter = IntIterators.wrap(values);
		else strbld = new StringBuilder();
		for ( double theta : new double[]{0.6, 1.0} ) {
			for ( boolean bLF : new boolean[] {false, true} ) {
				for ( boolean bPF : new boolean[] {false, true} ) {
					for ( int m : new int[] {1, 2} ) {
						PrefixSearch alg0 = new PrefixSearch(theta, bLF, bPF, IndexChoice.values()[m]);
						alg0.run(dataset);
						if (onTest) {
							for ( String attr : attrs ) 
								assertEquals(iter.nextInt(), Integer.parseInt(alg0.getStatContainer().getStat(attr)) );
						}
						else {
							for ( String attr : attrs ) strbld.append(alg0.getStatContainer().getStat(attr)+", ");
							strbld.append('\n');
						}
					}
				}
			}
		}
		if (!onTest) System.out.println(strbld.toString());
	}

}

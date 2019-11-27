package snu.kdd.substring_syn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Stat;

public class PrefixSearchRefactorCheckTest {
	

	@Test
	public void test() {
		int[] values = {
				49, 58, 17964598, 17430249, 49, 58, 1064550, 975201, 49, 58, 183021, 1308953, 49, 58, 47004, 217449, 49, 58, 15355882, 12373547, 49, 58, 937854, 630640, 49, 58, 153402, 917493, 49, 58, 42328, 133900, 8, 8, 17971381, 17449938, 8, 8, 7713, 2900, 8, 8, 5996, 200980, 8, 8, 156, 702, 8, 8, 13004655, 8875780, 8, 8, 6449, 1178, 8, 8, 3117, 100678, 8, 8, 115, 137, 
		};
		IntIterator iter = IntIterators.wrap(values);
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");
		StringBuilder strbld = new StringBuilder();
		for ( double theta : new double[]{0.6, 1.0} ) {
			for ( boolean bLF : new boolean[] {false, true} ) {
				for ( boolean bPF : new boolean[] {false, true} ) {
					for ( int m : new int[] {1, 2} ) {
						PrefixSearch alg0 = new PrefixSearch(theta, bLF, bPF, IndexChoice.values()[m]);
						alg0.run(dataset);
//						strbld.append(alg0.getStatContainer().getStat(Stat.Num_QS_Result)+", ");
//						strbld.append(alg0.getStatContainer().getStat(Stat.Num_TS_Result)+", ");
//						strbld.append(alg0.getStatContainer().getStat(Stat.Num_QS_Verified)+", ");
//						strbld.append(alg0.getStatContainer().getStat(Stat.Num_TS_Verified)+", ");
						assertEquals(iter.nextInt(), Integer.parseInt(alg0.getStatContainer().getStat(Stat.Num_QS_Result)) );
						assertEquals(iter.nextInt(), Integer.parseInt(alg0.getStatContainer().getStat(Stat.Num_TS_Result)) );
						assertEquals(iter.nextInt(), Integer.parseInt(alg0.getStatContainer().getStat(Stat.Num_QS_Verified)) );
						assertEquals(iter.nextInt(), Integer.parseInt(alg0.getStatContainer().getStat(Stat.Num_TS_Verified)) );
					}
				}
			}
		}
//		System.out.println(strbld.toString());
	}

}

/*
 * 49, 58, 17964598, 17430249, 49, 58, 1064550, 975201, 49, 58, 183021, 1308953, 49, 58, 47004, 217449, 49, 58, 15355882, 12373547, 49, 58, 937854, 630640, 49, 58, 153402, 917493, 49, 58, 42328, 133900, 8, 8, 17971381, 17449938, 8, 8, 7713, 2900, 8, 8, 5996, 200980, 8, 8, 156, 702, 8, 8, 13004655, 8875780, 8, 8, 6449, 1178, 8, 8, 3117, 100678, 8, 8, 115, 137, 
 */
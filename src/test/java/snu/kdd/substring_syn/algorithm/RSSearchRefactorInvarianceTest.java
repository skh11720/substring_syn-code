package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.RSSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Stat;

public class RSSearchRefactorInvarianceTest {
	

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
				66, 63, 66, 22562031, 22265313, 277192901, 273228623, 1487022, 1470847, 0, 0, 0, 0, 
				66, 63, 66, 1789940, 1649084, 25925231, 23464721, 98215, 93279, 0, 0, 0, 0, 
				66, 63, 66, 28724, 78367, 447650, 209736, 2388, 27934, 0, 0, 0, 0, 
				66, 63, 66, 28724, 43929, 447650, 150199, 2388, 10662, 0, 0, 0, 0, 
				66, 63, 66, 214867, 1620151, 2382070, 22082255, 1487022, 1470847, 0, 0, 2382070, 22082255, 
				66, 63, 66, 53752, 287190, 722070, 4224979, 98215, 93279, 0, 0, 722070, 4224979, 
				66, 63, 66, 8963, 35639, 191317, 101058, 2388, 27934, 0, 0, 191317, 101058, 
				66, 63, 66, 8963, 19539, 191317, 70841, 2388, 10662, 0, 0, 191317, 70841, 
				66, 63, 66, 8423870, 14706464, 50355394, 96858938, 1487022, 1470847, 50355394, 106651660, 0, 0, 
				66, 63, 66, 578009, 993366, 3502329, 6878642, 98215, 93279, 3502329, 7602186, 0, 0, 
				66, 63, 66, 9282, 78228, 59311, 207905, 2388, 27934, 59311, 208868, 0, 0, 
				66, 63, 66, 9282, 43790, 59311, 148368, 2388, 10662, 59311, 149331, 0, 0, 
				66, 63, 66, 80452, 1100239, 422449, 8044986, 1487022, 1470847, 50355394, 90111824, 422449, 8044986, 
				66, 63, 66, 19514, 185006, 106690, 1381527, 98215, 93279, 3502329, 16439188, 106690, 1381527, 
				66, 63, 66, 1822, 35626, 11905, 100890, 2388, 27934, 59311, 398892, 11905, 100890, 
				66, 63, 66, 1822, 19526, 11905, 70673, 2388, 10662, 59311, 329432, 11905, 70673, 
				11, 11, 11, 22573706, 22293372, 277336838, 273660304, 1487022, 1470847, 0, 0, 0, 0, 
				11, 11, 11, 9272, 1632, 134625, 20268, 623, 432, 0, 0, 0, 0, 
				11, 11, 11, 4309, 3024, 67868, 6781, 346, 1276, 0, 0, 0, 0, 
				11, 11, 11, 4309, 201, 67868, 754, 346, 95, 0, 0, 0, 0, 
				11, 11, 11, 4793, 232679, 30035, 3093386, 1487022, 1470847, 0, 0, 30035, 3093386, 
				11, 11, 11, 587, 604, 8649, 10330, 623, 432, 0, 0, 8649, 10330, 
				11, 11, 11, 556, 1142, 8479, 2983, 346, 1276, 0, 0, 8479, 2983, 
				11, 11, 11, 556, 96, 8479, 424, 346, 95, 0, 0, 8479, 424, 
				11, 11, 11, 2045983, 10348800, 11317250, 46562271, 1487022, 1470847, 11317250, 55604333, 0, 0, 
				11, 11, 11, 1193, 1799, 7205, 8496, 623, 432, 7205, 10250, 0, 0, 
				11, 11, 11, 274, 3014, 1500, 6717, 346, 1276, 1500, 6759, 0, 0, 
				11, 11, 11, 274, 195, 1500, 714, 346, 95, 1500, 732, 0, 0, 
				11, 11, 11, 593, 122996, 3224, 581077, 1487022, 1470847, 11317250, 21466460, 3224, 581077, 
				11, 11, 11, 53, 210, 297, 1347, 623, 432, 7205, 15734, 297, 1347, 
				11, 11, 11, 50, 1142, 282, 2983, 346, 1276, 1500, 9853, 282, 2983, 
				11, 11, 11, 50, 96, 282, 424, 346, 95, 1500, 1502, 282, 424, 
		}; // commit d4c6474, RSSearch6.30

		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "31622", "5");
		IntIterator iter = null;
		StringBuilder strbld = null;
		if (onTest) iter = IntIterators.wrap(values);
		else strbld = new StringBuilder();
		for ( double theta : new double[]{0.6, 1.0} ) {
			for ( boolean bLF : new boolean[] {false, true} ) {
				for ( boolean bPF : new boolean[] {false, true} ) {
					for ( IndexChoice indexChoice : new IndexChoice[] {IndexChoice.Naive, IndexChoice.Count, IndexChoice.Position, IndexChoice.CountPosition}) {
						RSSearch alg0 = new RSSearch(theta, bLF, bPF, indexChoice);
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

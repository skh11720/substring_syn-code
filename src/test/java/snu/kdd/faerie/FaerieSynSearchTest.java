package snu.kdd.faerie;

import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Stat;

public class FaerieSynSearchTest {

	@Test
	public void testSingleRun() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "10000", "3");
		double theta = 0.6;
		AbstractSearch alg1 = new FaerieSynSearch(theta, true);
		alg1.run(dataset);
	}

	@Test
	public void testCorrectness() throws IOException {

		String[] attrs = {
				Stat.Num_Result,
				Stat.Num_QS_Result,
				Stat.Num_TS_Result,
				Stat.Num_QS_Verified,
				Stat.Num_TS_Verified,
				Stat.Len_QS_Verified,
				Stat.Len_TS_Verified,
//				Stat.Len_QS_Retrieved,
//				Stat.Len_TS_Retrieved,
//				Stat.Len_QS_LF,
//				Stat.Len_TS_LF,
//				Stat.Len_QS_PF,
//				Stat.Len_TS_PF,
		};
		StringBuilder strbld = new StringBuilder();

		for ( Dataset dataset : TestDatasetManager.getAllDatasets("10000", "1000") ) {
			for ( double theta : new double[] {0.6, 1.0} ) {
				AbstractSearch alg0 = new FaerieSynNaiveSearch(theta);
				alg0.run(dataset);
				AbstractSearch alg1 = new FaerieSynSearch(theta, true);
				alg1.run(dataset);
				
				double t0 = Double.parseDouble(alg0.getStatContainer().getStat(Stat.Time_Total));
				double t1 = Double.parseDouble(alg1.getStatContainer().getStat(Stat.Time_Total));
				strbld.append(dataset.name+"\t"+Stat.Time_Total+"\t"+t0+"\t"+t1+"\n");
				
				for ( String attr : attrs ) {
					int val0 = Integer.parseInt(alg0.getStatContainer().getStat(attr));
					int val1 = Integer.parseInt(alg1.getStatContainer().getStat(attr));
//					assertEquals(val0, val1);
					strbld.append(dataset.name+"\t"+attr+"\t"+val0+"\t"+val1+"\t");
					if ( attr.equals(Stat.Num_Result) ) strbld.append((val0 == val1)+"\n");
					else strbld.append("\n");
				}
			}
		}
		System.out.println(strbld.toString());
		PrintStream ps = new PrintStream("tmp/FaerieSynSearchTest.testCorrectness.txt");
		ps.println(strbld.toString());
		ps.close();
	}
}

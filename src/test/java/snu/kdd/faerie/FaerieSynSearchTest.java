package snu.kdd.faerie;

import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class FaerieSynSearchTest {

	@Test
	public void test00SingleRun() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "3162", "3", "0.6");
		double theta = 0.6;
		AbstractSearch alg1 = new FaerieSynSearch(theta, false);
		alg1.run(dataset);
	}

	@Test
	public void test01Correctness() throws IOException {

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
	
	@Test
	public void test02CompareMemAndDIsk() {
		/*
		Time_Total	2792.084	9995.023
		Time_BuildIndex	84.329	167.555
		 */
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "1000", "1000", "3");
		double theta = 0.6;
		AbstractSearch alg0 = new FaerieSynSearch(theta, false);
		alg0.run(dataset);
		AbstractSearch alg1 = new FaerieSynSearch(theta, true);
		alg1.run(dataset);
		for ( String attr : new String[] {Stat.Time_Total, Stat.Time_BuildIndex, Stat.Num_Result, Stat.Num_QS_Result, Stat.Num_TS_Result}) {
			System.out.println(attr+"\t"+alg0.getStatContainer().getStat(attr)+"\t"+alg1.getStatContainer().getStat(attr));
		}
	}
	
	@Test
	public void test03VaryLenRatio() {
		/*	WIKI_n10000_r3162_q3, theta=0.6, isDiskBased=false
			0.2	10040.190	903	901	903
			0.4	14074.271	2547	2539	2547
			0.6	17796.640	4129	4109	4129
			0.8	22864.813	5617	5588	5617
			1.0	29560.680	7129	7090	7129

			WIKI_n10000_r3162_q3, theta=1.0, isDiskBased=false
			0.2	9393.085	12	12	12
			0.4	13991.276	46	46	46
			0.6	17891.154	89	87	89
			0.8	22450.968	123	121	123
			1.0	28867.628	195	193	195
		 */
		Log.disable();
		for ( String lenRatio : new String[] {"0.2", "0.4", "0.6", "0.8", "1.0"}) {
			Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "3162", "3", lenRatio);
			double theta = 0.6;
			AbstractSearch alg1 = new FaerieSynSearch(theta, true);
			alg1.run(dataset);
			System.out.println(lenRatio
					+"\t"+alg1.getStatContainer().getStat(Stat.Time_Total)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_Result)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_QS_Result)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_TS_Result));
		}
	}
}

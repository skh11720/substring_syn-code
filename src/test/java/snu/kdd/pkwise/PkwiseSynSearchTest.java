package snu.kdd.pkwise;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintStream;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import snu.kdd.faerie.FaerieSynNaiveSearch;
import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PkwiseSynSearchTest {

	@Test
	public void test00SingleRun() {
		double theta = 1.0;
		int qlen = 5;
		int kmax = 1;
		Dataset dataset = TestDatasetManager.getTransWindowDataset("AMAZON", "10000", "1000", ""+qlen, "1.0", theta);
		AbstractSearch alg1 = new PkwiseSynSearch(theta, qlen, kmax);
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

		for ( double theta : new double[] {0.6, 1.0} ) {
			for ( String name : new String[] {"WIKI", "PUBMED", "AMAZON"} ) {
				for ( int qlen : new int[] {1, 3, 5} ) {
					int kmax = qlen>1 ? 2 : 1;
					TransWindowDataset dataset = TestDatasetManager.getTransWindowDataset(name, "10000", "1000", ""+qlen, "1.0", theta);
					AbstractSearch alg0 = new FaerieSynNaiveSearch(theta);
					alg0.run(dataset);
					AbstractSearch alg1 = new PkwiseSynSearch(theta, qlen, kmax);
					alg1.run(dataset);
					
					double t0 = Double.parseDouble(alg0.getStatContainer().getStat(Stat.Time_Total));
					double t1 = Double.parseDouble(alg1.getStatContainer().getStat(Stat.Time_Total));
					strbld.append(dataset.name+"\t"+Stat.Time_Total+"\t"+t0+"\t"+t1+"\n");
					
					for ( String attr : attrs ) {
						int val0 = Integer.parseInt(alg0.getStatContainer().getStat(attr));
						int val1 = Integer.parseInt(alg1.getStatContainer().getStat(attr));
						if ( attr.equals(Stat.Num_Result) ) assertEquals(val0, val1);
						else if ( attr.equals(Stat.Num_QS_Result) ) assertEquals(val0, val1);
						else if ( attr.equals(Stat.Num_TS_Result) ) assertEquals(val0, val1);
						strbld.append(dataset.name+"\t"+attr+"\t"+val0+"\t"+val1+"\t");
						if ( attr.equals(Stat.Num_Result) ) strbld.append((val0 == val1)+"\n");
						else strbld.append("\n");
					}
				}
			}
		}
		System.out.println(strbld.toString());
		PrintStream ps = new PrintStream("tmp/PkwiseSynSearchTest.testCorrectness.txt");
		ps.println(strbld.toString());
		ps.close();
	}
	
	@Test
	public void test02IndexSize() {
		/*
		WIKI_n10000_r3162_q5_l0.6	theta=0.6	lenRatio=0.6	indexSize=26944352
		WIKI_n10000_r3162_q5_l1.0	theta=0.6	lenRatio=1.0	indexSize=61276246
		WIKI_n10000_r3162_q5_l0.6	theta=1.0	lenRatio=0.6	indexSize=3101963
		WIKI_n10000_r3162_q5_l1.0	theta=1.0	lenRatio=1.0	indexSize=6546376
		 */
		Log.disable();
		int qlen = 5;
		int kmax= 2;
		for ( double theta : new double[] {0.6, 1.0} ) {
			for ( String lenRatio : new String[] {"0.6", "1.0"} ) {
				Dataset dataset = TestDatasetManager.getTransWindowDataset("WIKI", "10000", "3162", ""+qlen, lenRatio, theta);
				PkwiseSynSearch alg = new PkwiseSynSearch(theta, qlen, kmax);
				alg.run(dataset);
				System.out.println(dataset.name+"\ttheta="+theta+"\tlenRatio="+lenRatio+"\tindexSize="+alg.diskSpaceUsage());
			}
		}
	}

	@Test
	public void test03VaryLenRatio() {
		/*	
		WIKI_n10000_r3162_q5_l0.2	theta=0.6	kmax=2	0.2	1758.363	66	61	66
		WIKI_n10000_r3162_q5_l0.4	theta=0.6	kmax=2	0.4	6384.191	175	170	175
		WIKI_n10000_r3162_q5_l0.6	theta=0.6	kmax=2	0.6	12979.315	269	260	269
		WIKI_n10000_r3162_q5_l0.8	theta=0.6	kmax=2	0.8	21229.815	383	372	383
		WIKI_n10000_r3162_q5_l1.0	theta=0.6	kmax=2	1.0	27381.245	494	481	494
		WIKI_n10000_r3162_q5_l0.2	theta=1.0	kmax=2	0.2	184.874	4	4	4
		WIKI_n10000_r3162_q5_l0.4	theta=1.0	kmax=2	0.4	956.023	28	28	28
		WIKI_n10000_r3162_q5_l0.6	theta=1.0	kmax=2	0.6	1826.178	58	58	58
		WIKI_n10000_r3162_q5_l0.8	theta=1.0	kmax=2	0.8	2756.018	81	81	81
		WIKI_n10000_r3162_q5_l1.0	theta=1.0	kmax=2	1.0	3771.643	100	100	100
		 */
		Log.disable();
		int qlen = 5;
		int kmax = 2;
		for ( double theta : new double[] {0.6, 1.0} ) {
			for ( String lenRatio : new String[] {"0.2", "0.4", "0.6", "0.8", "1.0"}) {
				Dataset dataset = TestDatasetManager.getTransWindowDataset("WIKI", "10000", "3162", ""+qlen, lenRatio, theta);
				AbstractSearch alg = new PkwiseSynSearch(theta, qlen, kmax);
				alg.run(dataset);
				System.out.println(dataset.name+"\ttheta="+theta+"\tkmax="+kmax
						+"\t"+lenRatio
						+"\t"+alg.getStatContainer().getStat(Stat.Time_Total)
						+"\t"+alg.getStatContainer().getStat(Stat.Num_Result)
						+"\t"+alg.getStatContainer().getStat(Stat.Num_QS_Result)
						+"\t"+alg.getStatContainer().getStat(Stat.Num_TS_Result));
			}
		}
	}
}

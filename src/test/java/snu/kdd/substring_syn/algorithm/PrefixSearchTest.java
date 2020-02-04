package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.FilterOptionLabel;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.algorithm.search.ExactPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class PrefixSearchTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	String[] versionList = {"2.00"};
	String latestVersion = "2.00";
	String name = "SPROT_long";

	@Test
	public void testSingle() throws IOException {
		DatasetParam param = new DatasetParam("AMAZON", "300000", "107836", "5", "1.0");
		double theta = 0.6;
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		AbstractSearch alg = new PositionPrefixSearch(theta, true, true, IndexChoice.CountPosition);
		alg.run(dataset);
	}
	
	@Test
	public void testExecutionWithEveryOption() throws IOException {
		DatasetParam param = new DatasetParam("AMAZON-DOC", "100", "1000", "5", "0.2");
		String argsTmpl = "-data AMAZON-DOC -alg PrefixSearch -nt 10 -nr 1000 -ql 5 -lr 1.0 -param theta:0.6,filter:%s";
		String outputTmpl = "%20s%20.6f%20.6f%20.6f%10d%10d%10d\n";
		StringBuilder strbld = new StringBuilder(String.format("%20s%20s%20s%10s%10s%10s\n", "FilterOption", "T_Total", "T_Qside", "T_Tside", "Num_Result", "Num_QS_Result", "Num_TS_Result"));
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		for ( AlgorithmFactory.FilterOptionLabel opt : FilterOptionLabel.values() ) {
//			if ( opt == FilterOptionLabel.Fopt_None ) continue;
			String[] args = String.format(argsTmpl, opt).split(" ");
			AbstractSearch alg = AlgorithmFactory.createInstance(new InputArgument(args));
			alg.run(dataset);
			strbld.append(String.format(outputTmpl, opt, 
					Double.parseDouble(alg.getStat(Stat.Time_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_QS_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_TS_Total)), 
					Integer.parseInt(alg.getStat(Stat.Num_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_QS_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_TS_Result))));
		}
		System.out.println(strbld.toString());
	}
	
	@Ignore
	public void testLengthFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = DatasetFactory.createInstanceByName(name, size);

		String[] results = new String[4];
		int i = 0;
		for ( boolean bLF: new boolean[]{false, true} ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, bLF, false, IndexChoice.Naive);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_Total);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_Total);
			results[i++] = String.format("%s\t%s\t%s\t%s", bLF, time_0, time_1, time_2);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "lf_query", "lf_text", Stat.Time_Total, Stat.Time_QS_Total, Stat.Time_TS_Total));
		for ( String result : results ) {
			System.out.println(result);
		}
	}
	
	@Ignore
	public void testIndexFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = DatasetFactory.createInstanceByName(name, size);

		String[] results = new String[4];
		int i = 0;
		for ( IndexChoice indexChoice : IndexChoice.values() ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, true, true, indexChoice);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_Total);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_Total);
			String time_5 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_IndexFilter);
			String time_6 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_IndexFilter);
			results[i++] = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", indexChoice.toString(), time_0, time_1, time_2, time_5, time_6);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "idxFilter_query", "idxFilter_text", Stat.Time_Total, Stat.Time_QS_Total, Stat.Time_TS_Total, Stat.Time_QS_IndexFilter, Stat.Time_TS_IndexFilter));
		for ( String result : results ) {
			System.out.println(result);
		}
	}

	public void test( String name, String size, String nr, String ql, double theta ) throws IOException {
		DatasetParam param = new DatasetParam(name, size, nr, ql, null);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(theta);
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.CountPosition);
		
		long ts = System.nanoTime();
		prefixSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
		assertTrue( isOutputCorrect(naiveSearch, prefixSearch, dataset) );
	}

	public boolean isOutputCorrect( ExactNaiveSearch naiveSearch, AbstractSearch prefixSearch, Dataset dataset ) throws IOException {
		BufferedReader br0 = new BufferedReader(new FileReader(naiveSearch.getOutputPath(dataset)));
		BufferedReader br1 = new BufferedReader(new FileReader(prefixSearch.getOutputPath(dataset)));
		Iterator<String> iter0 = br0.lines().iterator();
		Iterator<String> iter1 = br1.lines().iterator();
		boolean b = true;
		while ( iter0.hasNext() ) {
			try {
				if ( !iter0.next().equals( iter1.next() ) ) {
					b = false;
					break;
				}
			} catch ( NoSuchElementException e ) {
				b = false;
			}
		}
		if ( iter1.hasNext() ) b = false;
		br0.close();
		br1.close();
		return b;
	}
	
	@Ignore
	public void testIndexImplComparison() throws IOException {
		String name = "SPROT_long";
		String size = "102";
		double theta = 0.6;
		Dataset dataset = DatasetFactory.createInstanceByName(name, size);
		
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.Count);
		prefixSearch.run(dataset);
		String num_qs0 = prefixSearch.getStatContainer().getStat(Stat.Num_QS_Result);
		String num_ts0 =prefixSearch.getStatContainer().getStat(Stat.Num_TS_Result);
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.CountPosition);
		prefixSearch.run(dataset);
		String num_qs1 = prefixSearch.getStatContainer().getStat(Stat.Num_QS_Result);
		String num_ts1 =prefixSearch.getStatContainer().getStat(Stat.Num_TS_Result);
		assertTrue(num_qs0.equals(num_qs1));
		assertTrue(num_ts0.equals(num_ts1));
	}

	@Test
	public void testVaryLenRatio() {
		/*	
		    WIKI_n10000_r3162_q3, theta=0.6
		    0.2	942.045	903	901	903
			0.4	828.984	2547	2539	2547
			0.6	1275.105	4129	4109	4129
			0.8	1520.456	5617	5588	5617
			1.0	1799.332	7129	7090	7129

			WIKI_n10000_r3162_q3, theta=1.0
			0.2	561.684	12	12	12
			0.4	691.665	46	46	46
			0.6	928.745	89	87	89
			0.8	936.643	123	121	123
			1.0	1107.630	195	193	195

		 */
		Log.disable();
		for ( String lenRatio : new String[] {"0.2", "0.4", "0.6", "0.8", "1.0"}) {
			Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "3162", "3", lenRatio);
			double theta = 1.0;
			AbstractSearch alg1 = new PositionPrefixSearch(theta, true, true, IndexChoice.CountPosition);
			alg1.run(dataset);
			System.out.println(lenRatio
					+"\t"+alg1.getStatContainer().getStat(Stat.Time_Total)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_Result)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_QS_Result)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_TS_Result));
		}
	}
}

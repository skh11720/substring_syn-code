package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.algorithm.search.ExactPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	String[] versionList = {"2.00"};
	String latestVersion = "2.00";
	String name = "SPROT_long";

	@Test
	public void testSingle() throws IOException {
		test("SPROT_long", 0.7, "100", "2.00");
//		test("WIKI_3", 0.8, "13657", "2.00");
	}
	
	@Ignore
	public void testLengthFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);

		String[] results = new String[4];
		int i = 0;
		for ( boolean bLF: new boolean[]{false, true} ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, bLF, false, IndexChoice.Naive);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_QSTotal);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_TSTotal);
			results[i++] = String.format("%s\t%s\t%s\t%s", bLF, time_0, time_1, time_2);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "lf_query", "lf_text", Stat.Time_Total, Stat.Time_QSTotal, Stat.Time_TSTotal));
		for ( String result : results ) {
			System.out.println(result);
		}
	}
	
	@Ignore
	public void testIndexFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);

		String[] results = new String[4];
		int i = 0;
		for ( IndexChoice indexChoice : IndexChoice.values() ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, true, true, indexChoice);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_QSTotal);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_TSTotal);
			String time_5 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_IndexFilter);
			String time_6 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_IndexFilter);
			results[i++] = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", indexChoice.toString(), time_0, time_1, time_2, time_5, time_6);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "idxFilter_query", "idxFilter_text", Stat.Time_Total, Stat.Time_QSTotal, Stat.Time_TSTotal, Stat.Time_QS_IndexFilter, Stat.Time_TS_IndexFilter));
		for ( String result : results ) {
			System.out.println(result);
		}
	}

	public void test( String name, double theta, String size, String version ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(theta);
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.Position);
		
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
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.Count);
		prefixSearch.run(dataset);
		String num_qs0 = prefixSearch.getStatContainer().getStat(Stat.Num_QS_Result);
		String num_ts0 =prefixSearch.getStatContainer().getStat(Stat.Num_TS_Result);
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.Position);
		prefixSearch.run(dataset);
		String num_qs1 = prefixSearch.getStatContainer().getStat(Stat.Num_QS_Result);
		String num_ts1 =prefixSearch.getStatContainer().getStat(Stat.Num_TS_Result);
		assertTrue(num_qs0.equals(num_qs1));
		assertTrue(num_ts0.equals(num_ts1));
	}
}

package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.NaiveSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"101", "102", "103", "104", "105"};
	String[] versionList = {"1.00", "1.01", "1.02", "1.03", "1.04", "1.05"};
	String latestVersion = "2.00";
	String name = "SPROT_long";

	@Test
	public void testSingle() throws IOException {
		test("SPROT_long", 0.7, "100", "2.00");
	}
	
	@Ignore
	public void testAll() throws IOException {
		testIteration(thetaList, sizeList, versionList);
	}
	
	@Ignore
	public void testAllTheta() throws IOException {
		for ( double theta : thetaList ) {
			test(name, theta, "100", latestVersion);
		}
	}
	
	@Ignore
	public void testAllVersions() throws IOException {
		double[] thetaList = {0.7};
		String[] sizeList = {"100"};
		testIteration(thetaList, sizeList, versionList);
	}
	
	public void testIteration( double[] thetaList, String[] sizeList, String[] versionList ) throws IOException {
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				for ( String version : versionList ) {
					test(name, theta, size, version);
				}
			}
		}
	}
	
	@Test
	public void testLengthFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);

		String[] results = new String[4];
		int i = 0;
		for ( boolean lf_text: new boolean[]{false, true} ) {
			for ( boolean lf_query : new boolean[]{false, true} ) {
				AbstractSearch prefixSearch = new PrefixSearch(theta, false, lf_query, lf_text);
				prefixSearch.run(dataset);
				String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_0_Total);
				String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_1_QSTotal);
				String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_2_TSTotal);
				results[i++] = String.format("%s\t%s\t%s\t%s\t%s", lf_query, lf_text, time_0, time_1, time_2);
			}
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "lf_query", "lf_text", Stat.Time_0_Total, Stat.Time_1_QSTotal, Stat.Time_2_TSTotal));
		for ( String result : results ) {
			System.out.println(result);
		}
	}
	
	@Test
	public void testIndexFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);

		String[] results = new String[2];
		int i = 0;
		for ( boolean idxFilter_query : new boolean[]{false, true} ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, idxFilter_query, true, true);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_0_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_1_QSTotal);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_2_TSTotal);
			String time_5 = prefixSearch.getStatContainer().getStat(Stat.Time_5_IndexFilter);
			String num_qs_idxFilter = prefixSearch.getStatContainer().getStat(Stat.Num_QS_IndexFiltered);
			results[i++] = String.format("%s\t%s\t%s\t%s\t%s\t%s", idxFilter_query, time_0, time_1, time_2, time_5, num_qs_idxFilter);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s\t%s", "idxFilter_query", Stat.Time_0_Total, Stat.Time_1_QSTotal, Stat.Time_2_TSTotal, Stat.Time_5_IndexFilter, Stat.Num_QS_IndexFiltered));
		for ( String result : results ) {
			System.out.println(result);
		}
	}

	public void test( String name, double theta, String size, String version ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);
		
		NaiveSearch naiveSearch = new NaiveSearch(theta);
		AbstractSearch prefixSearch = null;
		if ( version.equals("2.00") ) prefixSearch = new PrefixSearch(theta, true, true, true);
		
		long ts = System.nanoTime();
		prefixSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
		assertTrue( isOutputCorrect(naiveSearch, prefixSearch, dataset) );
	}

	public boolean isOutputCorrect( NaiveSearch naiveSearch, AbstractSearch prefixSearch, Dataset dataset ) throws IOException {
		BufferedReader br0 = new BufferedReader(new FileReader(naiveSearch.getOutputPath(dataset)));
		BufferedReader br1 = new BufferedReader(new FileReader(prefixSearch.getOutputPath(dataset)));
		Iterator<String> iter0 = br0.lines().iterator();
		Iterator<String> iter1 = br1.lines().iterator();
		boolean b = true;
		while ( iter0.hasNext() && iter1.hasNext() ) {
			if ( !iter0.next().equals( iter1.next() ) ) {
				b = false;
				break;
			}
		}
		br0.close();
		br1.close();
		return b;
	}
}

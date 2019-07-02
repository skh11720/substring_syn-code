package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.NaiveSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_02;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_03;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_04;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_05;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_06;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_01;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"101", "102", "103", "104", "105"};
	String[] versionList = {"1.00", "1.01", "1.02", "1.03", "1.04", "1.05"};
	String currVersion = "1.05";
	String name = "SPROT_long";

	@Test
	public void testSingle() throws IOException {
		test("SYN_test_01", 0.9, "100", "1.06");
	}
	
	@Ignore
	public void testAll() throws IOException {
		testIteration(thetaList, sizeList, versionList);
	}
	
	@Ignore
	public void testAllTheta() throws IOException {
		for ( double theta : thetaList ) {
			test(name, theta, "100", currVersion);
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

	public void test( String name, double theta, String size, String version ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);
		
		NaiveSearch naiveSearch = new NaiveSearch(theta);
		PrefixSearch prefixSearch = null;
		if ( version.equals("1.00") ) prefixSearch = new PrefixSearch(theta);
		else if ( version.equals("1.01") ) prefixSearch = new PrefixSearch1_01(theta);
		else if ( version.equals("1.02") ) prefixSearch = new PrefixSearch1_02(theta);
		else if ( version.equals("1.03") ) prefixSearch = new PrefixSearch1_03(theta);
		else if ( version.equals("1.04") ) prefixSearch = new PrefixSearch1_04(theta);
		else if ( version.equals("1.05") ) prefixSearch = new PrefixSearch1_05(theta);
		else if ( version.equals("1.06") ) prefixSearch = new PrefixSearch1_06(theta);
		
		long ts = System.nanoTime();
		prefixSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
		assertTrue( isOutputCorrect(naiveSearch, prefixSearch, dataset) );
	}

	public boolean isOutputCorrect( NaiveSearch naiveSearch, PrefixSearch prefixSearch, Dataset dataset ) throws IOException {
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

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
import snu.kdd.substring_syn.algorithm.search.PrefixSearch1_01;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	@Ignore
	public void testAllTheta() throws IOException {
		double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
		for ( double theta : thetaList ) {
			test(theta);
		}
	}
	
	@Ignore
	public void testSingle() throws IOException {
		test(0.7);
	}
	
	@Test
	public void testMultiVersions() throws IOException {
		double theta = 0.7;
//		test(theta, "1.00");
//		test(theta, "1.01");
//		test(theta, "1.02");
		test(theta, "1.03");
	}

	public void test( double theta ) throws IOException {
		test(theta, "1.00");
	}
	
	public void test( double theta, String version ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "100");
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);
		
		NaiveSearch naiveSearch = new NaiveSearch(theta);
		PrefixSearch prefixSearch = null;
		if ( version.equals("1.00") ) prefixSearch = new PrefixSearch(theta);
		else if ( version.equals("1.01") ) prefixSearch = new PrefixSearch1_01(theta);
		else if ( version.equals("1.02") ) prefixSearch = new PrefixSearch1_02(theta);
		else if ( version.equals("1.03") ) prefixSearch = new PrefixSearch1_03(theta);
		
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

package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.NaiveSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class NaiveSearchTest {
	
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};

	@Test
	public void testAll() throws IOException {
		for ( String size : sizeList ) {
			for ( double theta : thetaList ) {
				test(theta, size);
			}
		}
	}
	
	@Test
	public void testSingle() throws IOException {
		test(0.6, "100");
	}
	
	public void test( double theta, String size ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);
		
		NaiveSearch naiveSearch = new NaiveSearch(theta);
		
		long ts = System.nanoTime();
		naiveSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
	}
}

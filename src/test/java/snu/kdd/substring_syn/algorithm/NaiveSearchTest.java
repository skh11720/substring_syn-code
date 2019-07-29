package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.NaiveSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Util;

public class NaiveSearchTest {
	
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};

	@Test
	public void testAll() throws IOException {
		for ( String size : sizeList ) {
			for ( double theta : thetaList ) {
				test("SPROT_long", theta, size);
			}
		}
	}
	
	@Test
	public void testSingle() throws IOException {
		test("SPROT_long", 0.9, "100");
	}
	
	public void test( String name, double theta, String size ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing(name, size);
		
		NaiveSearch naiveSearch = new NaiveSearch(theta);
		
		long ts = System.nanoTime();
		naiveSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
	}
}

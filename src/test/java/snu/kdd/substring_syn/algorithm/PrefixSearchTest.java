package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	@Test
	public void testAllTheta() throws IOException {
		double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
		for ( double theta : thetaList ) {
			test(theta);
		}
	}
	
	@Test
	public void testSingle() throws IOException {
		test(0.7);
	}
	
	public void test( double theta ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", 100);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);
		
		PrefixSearch prefixSearch = new PrefixSearch(theta);
		
		long ts = System.nanoTime();
		prefixSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
		assertTrue( isOutputCorrect(theta) );
	}

	public boolean isOutputCorrect( double theta ) throws IOException {
		BufferedReader br0 = new BufferedReader(new FileReader(String.format("output/NaiveSearch_1.00_%.2f.txt", theta)));
		BufferedReader br1 = new BufferedReader(new FileReader(String.format("output/PrefixSearch_1.00_%.2f.txt", theta)));
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

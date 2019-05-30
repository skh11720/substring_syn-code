package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.NaiveSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class NaiveSearchTest {

	@Test
	public void test() throws IOException {
		double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
		for ( double theta : thetaList ) {
			Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", 100);
			TokenOrder order = new TokenOrder(dataset);
			dataset.reindexByOrder(order);
			
			NaiveSearch naiveSearch = new NaiveSearch(theta);
			
			long ts = System.nanoTime();
			naiveSearch.run(dataset);
			long t = System.nanoTime() - ts;
			System.out.println(t/1e6);
		}
	}
}

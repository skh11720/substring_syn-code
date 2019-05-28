package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Query;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	@Test
	public void test() throws IOException {
		double theta = 0.6;
		Query query = Util.getQueryWithPreprocessing("SPROT_long", 100);
		TokenOrder order = new TokenOrder(query);
		query.reindexByOrder(order);
		
		PrefixSearch prefixSearch = new PrefixSearch(theta);
		
		long ts = System.nanoTime();
		prefixSearch.run(query);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
	}
}

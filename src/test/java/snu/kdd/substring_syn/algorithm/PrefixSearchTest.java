package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Util;

public class PrefixSearchTest {

	@Test
	public void test() throws IOException {
		double theta = 0.6;
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
		File file0 = new File(String.format("output/NaiveSearch_1.00_%.2f.txt", theta));
		File file1 = new File(String.format("output/PrefixSearch_1.00_%.2f.txt", theta));
		return FileUtils.contentEquals(file0, file1);
	}
}

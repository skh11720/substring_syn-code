package snu.kdd.substring_syn;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.faerie.FaerieSynSearch;
import snu.kdd.pkwise.PkwiseSynSearch;
import snu.kdd.pkwise.TransWindowDataset;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Stat;

public class DryRunTest {
	
	String datasetName = "WIKI";
	String size = "1000";
	String nr = "1000";
	String qlen = "5";
	String lenRatio = "0.8";
	double theta = 0.6;
	StringBuilder strbld = new StringBuilder();

	@Test
	public void test() {
		/*
		PrefixSearch	515.758	2.542	35	36
		PkwiseSynSearch	3808.977	1.143	35	36
		FaerieSynSearch	9712.986	95.176	35	36
		 */
		runPrefixSearch();
		runPkwiseSynSearch();
		runFaerieSynSearch();
		System.out.println(strbld.toString());
	}

	public void runPrefixSearch() {
		Dataset dataset = TestDatasetManager.getDataset(datasetName, size, nr, qlen, lenRatio);
		PrefixSearch alg = new PrefixSearch(theta, true, true, IndexChoice.CountPosition);
		alg.run(dataset);
		strbld.append("PrefixSearch\t"+getSummary(alg)+"\n");
	}
	
	public void runPkwiseSynSearch() {
		int kmax = 2;
		TransWindowDataset dataset = TestDatasetManager.getTransWindowDataset(datasetName, size, nr, qlen, lenRatio, theta);
		PkwiseSynSearch alg = new PkwiseSynSearch(theta, Integer.parseInt(qlen), kmax);
		alg.run(dataset);
		strbld.append("PkwiseSynSearch\t"+getSummary(alg)+"\n");
	}
	
	public void runFaerieSynSearch() {
		Dataset dataset = TestDatasetManager.getDataset(datasetName, size, nr, qlen, lenRatio);
		FaerieSynSearch alg = new FaerieSynSearch(theta, true);
		alg.run(dataset);
		strbld.append("FaerieSynSearch\t"+getSummary(alg)+"\n");
	}
	
	private String getSummary(AbstractSearch alg) {
		return ""
				+alg.getStat(Stat.Time_Total)+"\t"
				+alg.getStat(Stat.Time_SearchPerQuery+"_MEAN")+"\t"
				+alg.getStat(Stat.Num_QS_Result)+"\t"
				+alg.getStat(Stat.Num_TS_Result);
	}
}

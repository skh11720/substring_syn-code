package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.faerie.FaerieSynContainmentSearch;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.ContainmentPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveContainmentSearch;
import snu.kdd.substring_syn.algorithm.search.NaiveContainmentSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class ContainmentSearchTest {

	double[] thetaList = {0.6, 0.8, 1.0};

	@Test
	public void testAll() throws IOException {
		Log.disable();
		String[] datanameArray = {"WIKI", "PUBMED", "AMAZON"};
		for ( String dataname : datanameArray ) {
			DatasetParam param = new DatasetParam(dataname, "10000", "1000", "5", "1.0");
			Dataset dataset = DatasetFactory.createInstanceByName(param);
			for ( double theta : thetaList ) {
				AbstractSearch[] algArray = new  AbstractSearch[3];
				algArray[0] = new ExactNaiveContainmentSearch(theta);
				algArray[1] = new NaiveContainmentSearch(theta);
				algArray[2] = new ContainmentPrefixSearch(theta, IndexChoice.Count);
				
				for ( AbstractSearch alg : algArray ) {
					alg.run(dataset);
					System.out.println(param.toString()+"\t"+String.format(".1f", theta)+"\t"+alg.getName()+"\t"+alg.getStat(Stat.Num_Result)+"\t"+alg.getStat(Stat.Num_QS_Result)+"\t"+alg.getStat(Stat.Num_TS_Result));
				}
			}
		}
	}
	
	@Test
	public void test() throws IOException {
		Log.disable();
		DatasetParam param = new DatasetParam("WIKI", "10000", "3162", "5", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		for ( double theta : thetaList ) {
			AbstractSearch alg0 = new ContainmentPrefixSearch(theta, IndexChoice.Count);
			alg0.run(dataset);
			AbstractSearch alg1 = new FaerieSynContainmentSearch(theta, true);
			alg1.run(dataset);
			System.out.println(param.toString()+"\t"+String.format(".1f", theta)+"\t"+alg0.getName()+"\t"+alg0.getStat(Stat.Num_Result)+"\t"+alg0.getStat(Stat.Num_QS_Result)+"\t"+alg0.getStat(Stat.Num_TS_Result));
			System.out.println(param.toString()+"\t"+String.format(".1f", theta)+"\t"+alg1.getName()+"\t"+alg1.getStat(Stat.Num_Result)+"\t"+alg1.getStat(Stat.Num_QS_Result)+"\t"+alg1.getStat(Stat.Num_TS_Result));
		}
	}
}

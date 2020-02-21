package snu.kdd.substring_syn.algorithm;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;

@RunWith(Parameterized.class)
public class NaiveSearchTest {
	
	Param param;
	
	static class Param {
		final double theta;
		final DatasetParam datasetParam;
		
		public Param( double theta, String name, String size, String qlen, String nr, String lr ) {
			this.theta = theta;
			this.datasetParam = new DatasetParam(name, size, nr, qlen, lr);
		}
	}
	
	@Parameters
	public static Collection<Param> provideParams() {
		ObjectList<Param> paramList = new ObjectArrayList<>();
		double[] thetaList = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
		String[] sizeList = {"100"};
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				paramList.add(new Param(theta, "WIKI", size, "5", "1000", "1.0"));
			}
		}
		return paramList;
	}
	
	public NaiveSearchTest( Param param ) {
		this.param = param;
	}

	@Test
	public void test() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName(param.datasetParam);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(param.theta);
		
		long ts = System.nanoTime();
		naiveSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
	}
}

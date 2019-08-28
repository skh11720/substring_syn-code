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

@RunWith(Parameterized.class)
public class NaiveSearchTest {
	
	Param param;
	
	static class Param {
		double theta;
		String size;
		String name = "SPROT_long";
		
		public Param( double theta, String size ) {
			this.theta = theta;
			this.size = size;
		}
	}
	
	@Parameters
	public static Collection<Param> provideParams() {
		ObjectList<Param> paramList = new ObjectArrayList<>();
		double[] thetaList = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
		String[] sizeList = {"100", "101", "102", "103", "104", "105"};
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				paramList.add( new Param(theta, size) );
			}
		}
		return paramList;
	}
	
	public NaiveSearchTest( Param param ) {
		this.param = param;
	}

	@Test
	public void test() throws IOException {
		Dataset dataset = Dataset.createInstanceByName(param.name, param.size);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(param.theta);
		
		long ts = System.nanoTime();
		naiveSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
	}
}

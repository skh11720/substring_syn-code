package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.ExactPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch.IndexChoice;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Util;

@RunWith(Parameterized.class)
public class PrefixSearchCorrectnessTest {
	
	Param param;
	
	static class Param {
		double theta;
		String size;
		String name = "SPROT_long";
		boolean bIF = true;
		boolean bICF = true;
		boolean bLF = true;
		boolean bPF = true;
		IndexChoice index_impl;
		
		public Param( double theta, String size, boolean bIF, boolean bICF, boolean bLF, boolean bPF, IndexChoice index_impl ) {
			this.theta = theta;
			this.size = size;
			this.bIF = bIF;
			this.bICF = bICF;
			this.bLF = bLF;
			this.bPF = bPF;
			this.index_impl = index_impl;
		}
	}
	
	@Parameters
	public static Collection<Param> provideParams() {
		ObjectList<Param> paramList = new ObjectArrayList<>();
		double[] thetaList = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
		String[] sizeList = {"100", "101", "102", "103", "104", "105"};
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				paramList.add( new Param(theta, size, false, false, false, false, IndexChoice.Naive) );
			}
		}
		return paramList;
	}
	
	public PrefixSearchCorrectnessTest( Param param ) {
		this.param = param;
	}

	@Test
	public void test() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing(param.name, param.size);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(param.theta);
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(param.theta, param.bIF, param.bICF, param.bLF, param.bPF, param.index_impl);
		
		long ts = System.nanoTime();
		prefixSearch.run(dataset);
		long t = System.nanoTime() - ts;
		System.out.println(t/1e6);
		assertTrue( isOutputCorrect(naiveSearch, prefixSearch, dataset) );
	}

	public boolean isOutputCorrect( ExactNaiveSearch naiveSearch, AbstractSearch prefixSearch, Dataset dataset ) throws IOException {
		BufferedReader br0 = new BufferedReader(new FileReader(naiveSearch.getOutputPath(dataset)));
		BufferedReader br1 = new BufferedReader(new FileReader(prefixSearch.getOutputPath(dataset)));
		Iterator<String> iter0 = br0.lines().iterator();
		Iterator<String> iter1 = br1.lines().iterator();
		boolean b = true;
		while ( iter0.hasNext() ) {
			try {
				if ( !iter0.next().equals( iter1.next() ) ) {
					b = false;
					break;
				}
			} catch ( NoSuchElementException e ) {
				b = false;
			}
		}
		if ( iter1.hasNext() ) b = false;
		br0.close();
		br1.close();
		return b;
	}
}

package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.algorithm.search.ExactPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch.IndexChoice;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Util;

@RunWith(Parameterized.class)
public class PrefixSearchFilterPowerTest {
	
	static PrintStream ps;
	Param param;
	
	static class Param {
		double theta;
		String size;
		String name = "SPROT_long";
		boolean bIF = true;
		boolean bLF = true;
		boolean bPF = true;
		IndexChoice index_impl;
		
		public Param( double theta, String size, boolean bIF, boolean bLF, boolean bPF, IndexChoice index_impl ) {
			this.theta = theta;
			this.size = size;
			this.bIF = bIF;
			this.bLF = bLF;
			this.bPF = bPF;
			this.index_impl = index_impl;
		}
	}
	
	@BeforeClass
	public static void setup() throws FileNotFoundException {
		ps = new PrintStream("tmp/PrefixSearchFilterPowerTest.txt");
	}
	
	@AfterClass
	public static void cleanup() {
		ps.close();
	}
	
	@Parameters
	public static Collection<Param> provideParams() {
		ObjectList<Param> paramList = new ObjectArrayList<>();
		double[] thetaList = {1.0, 0.8, 0.6};
		String[] sizeList = {"100"};
		boolean[][] optionList = {
//				{false, false, false, false}, 
//				{false, false, false, true}, 
//				{true, false, false, true}, 
//				{true, true, false, true}, 
//				{true, false, true, true}, 
				{true, true, true, true}};
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				for ( boolean[] option : optionList ) {
					paramList.add( new Param(theta, size, option[0], option[1], option[2], option[3]?IndexChoice.Position:IndexChoice.Naive) );
				}
			}
		}
		return paramList;
	}
	
	public PrefixSearchFilterPowerTest( Param param ) {
		this.param = param;
	}

	@Test
	public void test() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing(param.name, param.size);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(param.theta);
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(param.theta, param.bIF, param.bLF, param.bPF, param.index_impl);
		
		prefixSearch.run(dataset);
		assertTrue( isOutputCorrect(naiveSearch, prefixSearch, dataset) );
		ps.println(prefixSearch.getStatContainer().outputSummaryString());
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

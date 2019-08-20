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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.algorithm.search.ExactPrefixSearch;
import snu.kdd.substring_syn.data.Dataset;

@RunWith(Parameterized.class)
public class PrefixSearchFilterPowerTest {
	
	static PrintStream ps;
	Param param;
	
	static class Param {
		double theta;
		String size;
		String name = "SPROT_long";
		boolean bLF = true;
		boolean bPF = true;
		IndexChoice index_impl;
		
		public Param( double theta, String size, boolean bLF, boolean bPF, IndexChoice index_impl ) {
			this.theta = theta;
			this.size = size;
			this.bLF = bLF;
			this.bPF = bPF;
			this.index_impl = index_impl;
		}
	}
	
	@Rule
	public Timeout globalTimeout = Timeout.seconds(5);
	
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
		int[][] optionList = {
				{0, 0, 0}, // NoFilter
				{0, 0, 1}, // IF
				{0, 0, 2}, // ICF
				{0, 0, 3}, // IPF
				{1, 0, 3}, // LF
				{1, 1, 3}, // PF
				{1, 1, 0}, // NoIndex
				};
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				for ( int[] option : optionList ) {
					
					paramList.add( new Param(theta, size, option[0]==1?true:false, option[1]==1?true:false, IndexChoice.values()[option[2]]) );
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
		Dataset dataset = Dataset.createInstanceByName(param.name, param.size);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(param.theta);
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(param.theta, param.bLF, param.bPF, param.index_impl);
		
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

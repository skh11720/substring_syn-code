package snu.kdd.substring_syn.algorithm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.ZeroPositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.ZeroPrefixSearch;
import snu.kdd.substring_syn.data.Dataset;

@RunWith(Parameterized.class)
public class PrefixSearchFilterPowerTest {
	
	static PrintStream ps;
	Param param;
	
	static class Param {
		final double theta;
		final String name;
		final String size;
		final String ql;
		final String nr;
		final boolean bLF;
		final boolean bPF;
		final IndexChoice index_impl;
		
		public Param( double theta, String name, String size, String ql, boolean bLF, boolean bPF, IndexChoice index_impl ) {
			this.theta = theta;
			this.name = name;
			this.size = size;
			this.ql = ql;
			if ( this.name.equals("PUBMED") ) this.nr = "79011";
			else this.nr = "107836";
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
		double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
		String[] nameList = {"WIKI", "PUBMED", "AMAZON"};
		String[] sizeList = {"100000"};
		String[] qlList = {"3", "5"};
		int[][] optionList = {
//				{0, 0, 0}, // NoFilter
//				{0, 0, 1}, // IF
//				{1, 1, 1}, // NaivePF
//				{0, 0, 2}, // ICF
//				{0, 0, 3}, // IPF
//				{1, 0, 3}, // LF
//				{1, 1, 3}, // PF
				
				{1, 0, 4}, // IPFOnly_L
				{1, 0, 2}, // ICF_L
				{0, 0, 4}, // IPFOnly
				{1, 0, 1}, // LFOnly
				{0, 1, 1}, // PFOnly 
				};
		for ( String ql : qlList ) {
			for ( double theta : thetaList ) {
				for (String name : nameList ) {
					for ( String size : sizeList ) {
						for ( int[] option : optionList ) {
							paramList.add( new Param(theta, name, size, ql, option[0]==1?true:false, option[1]==1?true:false, IndexChoice.values()[option[2]]) );
						}
					}
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
		Dataset dataset = Dataset.createInstanceByName(param.name, param.size, param.nr, param.ql);
		
		AbstractSearch prefixSearch = null;
		if ( param.index_impl == IndexChoice.CountPosition ) prefixSearch = new ZeroPositionPrefixSearch(param.theta, param.bLF, param.bPF, param.index_impl);
		else prefixSearch = new ZeroPrefixSearch(param.theta, param.bLF, param.bPF, param.index_impl);
		
		prefixSearch.run(dataset);
		ps.println(prefixSearch.getStatContainer().outputSummaryString());
	}
}

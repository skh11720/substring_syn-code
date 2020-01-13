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
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.FilterOption;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.FilterOptionLabel;
import snu.kdd.substring_syn.algorithm.search.ZeroPositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.ZeroPrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;

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
		FilterOption[] optionList = {
				new FilterOption(FilterOptionLabel.Fopt_None),
				new FilterOption(FilterOptionLabel.Fopt_Index),
				new FilterOption(FilterOptionLabel.Fopt_C),
				new FilterOption(FilterOptionLabel.Fopt_P),
				new FilterOption(FilterOptionLabel.Fopt_L),
				new FilterOption(FilterOptionLabel.Fopt_R),
				new FilterOption(FilterOptionLabel.Fopt_CP),
				new FilterOption(FilterOptionLabel.Fopt_CL),
				new FilterOption(FilterOptionLabel.Fopt_PL),
				new FilterOption(FilterOptionLabel.Fopt_CPL),
				new FilterOption(FilterOptionLabel.Fopt_CPLR),
		};
		for ( String ql : qlList ) {
			for ( double theta : thetaList ) {
				for (String name : nameList ) {
					for ( String size : sizeList ) {
						for ( FilterOption opt : optionList ) {
							paramList.add( new Param(theta, name, size, ql, opt.bLF, opt.bPF, opt.indexChoice) );
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
		DatasetParam dParam = new DatasetParam(param.name, param.size, param.nr, param.ql, null);
		Dataset dataset = DatasetFactory.createInstanceByName(dParam);
		
		AbstractSearch prefixSearch = null;
		if ( param.index_impl == IndexChoice.CountPosition ) prefixSearch = new ZeroPositionPrefixSearch(param.theta, param.bLF, param.bPF, param.index_impl);
		else prefixSearch = new ZeroPrefixSearch(param.theta, param.bLF, param.bPF, param.index_impl);
		
		prefixSearch.run(dataset);
		ps.println(prefixSearch.getStatContainer().outputSummaryString());
	}
}

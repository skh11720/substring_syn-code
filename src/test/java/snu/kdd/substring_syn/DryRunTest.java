package snu.kdd.substring_syn;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory;
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
	public void test() throws IOException, ParseException {
		/*
		PrefixSearch	515.758	2.542	35	36
		PkwiseSynSearch	3808.977	1.143	35	36
		FaerieSynSearch	9712.986	95.176	35	36
		 */
		runAlgorithm("PrefixSearch", "theta:0.6,filter:Fopt_CPLR");
		runAlgorithm("PkwiseSynSearch", "theta:0.6,kmax:2");
		runAlgorithm("FaerieSynSearch", "theta:0.6,isDiskBased:true");
		System.out.println(strbld.toString());
	}
	
	public void runAlgorithm(String algName, String algOption) throws IOException, ParseException {
		CommandLine cmd = getCmd(algName, algOption);
		Dataset dataset = Dataset.createInstance(cmd);
		AbstractSearch alg = AlgorithmFactory.createInstance(cmd);
		alg.run(dataset);
		strbld.append(algName+"\t"+getSummary(alg)+"\n");
	}

	private String getSummary(AbstractSearch alg) {
		return ""
				+alg.getStat(Stat.Time_Total)+"\t"
				+alg.getStat(Stat.Time_SearchPerQuery+"_MEAN")+"\t"
				+alg.getStat(Stat.Num_QS_Result)+"\t"
				+alg.getStat(Stat.Num_TS_Result);
	}

	private CommandLine getCmd(String algName, String algOption) throws ParseException {
		String[] args = ("-data WIKI -alg "+algName+" -nt 1000 -nr 1000 -ql 5 -lr 0.8 -param "+algOption).split(" ");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( App.argOptions, args, false );
		return cmd;
	}
}

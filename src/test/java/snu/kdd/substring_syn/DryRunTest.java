package snu.kdd.substring_syn;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

import junit.framework.TestCase;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Stat;

public class DryRunTest extends TestCase {
	
	String datasetName = "WIKI";
	String size = "1000";
	String nr = "1000";
	String qlen = "5";
	String lenRatio = "1.0";
	String nar = "-1";
	String theta = "0.6";
	StringBuilder strbld = new StringBuilder();

	@Test
	public void test() throws IOException, ParseException {
		/*
		RSSearch	544.388	2.900	60	60
		PkwiseSynSearch	4450.524	3.354	60	60
		FaerieSynSearch	9679.844	95.270	60	60
		 */
		runAlgorithm("RSSearch", "theta:"+theta+",filter:Fopt_CPLR");
		runAlgorithm("PkwiseSynSearch", "theta:"+theta+",kmax:opt");
		runAlgorithm("FaerieSynSearch", "theta:"+theta+",isDiskBased:true");
		System.out.println(strbld.toString());
	}
	
	public void runAlgorithm(String algName, String algOption) throws IOException, ParseException {
		InputArgument arg = getArgument(algName, algOption);
		Dataset dataset = DatasetFactory.createInstance(arg);
		AbstractSearch alg = AlgorithmFactory.createInstance(arg);
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

	private InputArgument getArgument(String algName, String algOption) throws ParseException {
		String[] args = (String.format("-data %s -alg %s -nt %s -nr %s -ql %s -lr %s -nar %s -param %s", datasetName, algName, size, nr, qlen, lenRatio, nar, algOption)).split(" ");
		return new InputArgument(args);
	}
}

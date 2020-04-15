package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.PositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.validator.AbstractGreedyValidator;
import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public class VerificationHeuristicTest {

	String data = "WIKI";
	String[] ntList = {"10000", "100000"};
	String nr = "107836";
	String ql = "3";
//	int nOpt = 9;
	int[] optArr = {0,1,2,3,4,5,6,7,8};

	@Test
	public void test() throws IOException {
		for ( String nt : ntList ) {
			singleTest(data, nt, nr, ql);
		}
	}
		
	public void singleTest( String data, String nt, String nr, String ql ) throws IOException {
		int[][] output = new int[optArr.length][3];
		DatasetParam param = new DatasetParam(data, nt, nr, ql, null);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		for ( int i=0; i<optArr.length; ++i ) {
			AbstractGreedyValidator.optScore = optArr[i];
			AbstractSearch prefixSearch = new PositionPrefixSearchWithVerifyOption(0.6, true, true, IndexChoice.CountPosition);
			prefixSearch.run(dataset);
			StatContainer stat = prefixSearch.getStatContainer();
			output[i][0] = Integer.parseInt(stat.getStat(Stat.Num_Result));
			output[i][1] = Integer.parseInt(stat.getStat(Stat.Num_QS_Result));
			output[i][2] = Integer.parseInt(stat.getStat(Stat.Num_TS_Result));
		}
		
		System.out.println("data: "+data);
		System.out.println("nt: "+nt);
		System.out.println("nr: "+nr);
		System.out.println("ql: "+ql);
		for ( int i=0; i<optArr.length; ++i ) {
			System.out.println(String.format("v%d\t%8d\t%8d\t%8d", optArr[i], output[i][0], output[i][1], output[i][2]));
		}
	}
}

class PositionPrefixSearchWithVerifyOption extends PositionPrefixSearch {

	public PositionPrefixSearchWithVerifyOption(double theta, boolean bLF, boolean bPF, IndexChoice indexChoice) {
		super(theta, bLF, bPF, indexChoice);
	}
	
	@Override
	public String getName() {
		return "PrefixSearch"+"-v"+GreedyValidator.optScore;
	}
}

/*

data: WIKI
nt: 10000
nr: 107836
ql: 3
v0	   13196	    7600	   13076
v1	   11773	    5721	   11657
v2	   11787	    5765	   11648
v3	   13206	    7510	   13076
v4	   13181	    7445	   13072
v5	   13173	    7445	   13064
v6	    9774	    7545	    9668
v7	    9776	    7546	    9669
v8	   13181	    7399	   13072


data: WIKI
nt: 100000
nr: 107836
ql: 3
v0	   96614	   73026	   95509
v1	  128076	   70754	  126549
v2	  127954	   70831	  126428
v3	  128519	   71963	  127194
v4	  128631	   71582	  127087
v5	  128481	   71582	  126932
v6	   94813	   72319	   93723
v7	   94834	   72327	   93737
v8	  128631	   71073	  127089



*** after decrement ***

data: WIKI
nt: 10000
nr: 107836
ql: 3
v0	   13197	    7654	   13065
v1	   13182	    7611	   13070
v2	   13178	    7610	   13067
v3	   13198	    7655	   13065
v4	   13182	    7611	   13070
v5	   13179	    7611	   13067
v6	   13198	    7655	   13065
v7	   13198	    7655	   13065
v8	   13182	    7611	   13070


data: WIKI
nt: 100000
nr: 107836
ql: 3
v0	  128509	   73240	  127174
v1	  128586	   73138	  127043
v2	  128477	   73138	  126934
v3	  128526	   73257	  127174
v4	  128602	   73154	  127043
v5	  128493	   73154	  126934
v6	  128526	   73257	  127174
v7	  128526	   73257	  127174
v8	  128602	   73154	  127043
*/
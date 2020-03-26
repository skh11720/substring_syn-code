package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.FilterOptionLabel;
import snu.kdd.substring_syn.algorithm.search.ExactNaiveSearch;
import snu.kdd.substring_syn.algorithm.search.ExactPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.PrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.RecordPool;
import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;

public class PrefixSearchTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	String[] versionList = {"2.00"};
	String latestVersion = "2.00";
	String name = "SPROT_long";

	@Test
	public void testSingle() throws IOException {
		DatasetParam param = new DatasetParam("AMAZON", "100000", "107836", "5", "1.0");
		double theta = 0.6;
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		AbstractSearch alg = new PositionPrefixSearch(theta, false, false, IndexChoice.CountPosition);
		alg.run(dataset);
	}
	
	@Test
	public void testExecutionWithEveryOption() throws IOException {
		/*
	   FilterOption     T_Total     T_Qside     T_Tside   N_Q_Ret   N_T_Ret   L_Q_Ret   L_T_Ret   N_Q_Val   N_T_Val   L_Q_Val   L_T_Val     N_Res   N_Q_Res
		  Fopt_None   17429.538    6984.718   10385.864     10000     10000    240000    240000   3777560   3774423  50161755  50080034         6         6         6
		 Fopt_Index   12914.061    4687.248    8141.496      5982      5982    154618    154626   2601706   2598733  36608792  36528855         6         6         6
			 Fopt_C    1671.293     413.560    1221.923       286       286      9781      9777    204459    201236   3417042   3334351         6         6         6
			 Fopt_P     205.801      53.868     106.850         6        75       245       182         6        86        20       202         6         6         6
			 Fopt_L    3778.054    1782.063    1978.831     10000     10000    240000    240000   1178576   1694226   6344647   7540504         6         6         6
			 Fopt_R    3859.786     711.166    3131.935     10000     10000    240000    240000     36517     44853    373926    431482         6         6         6
			Fopt_IL    2429.788    1115.719    1280.668      5982      5982    154618    154626    770730   1101314   4155860   4907042         6         6         6
			Fopt_IR    3399.511     458.620    2904.551      5982      5982    154618    154626     36517     44853    373926    431482         6         6         6
			Fopt_CP      57.435      11.258      11.163         6        19       245       100         6        20        20       102         6         6         6
			Fopt_CL     246.135     103.799     113.330       286       286      9781      9777     50628     70796    275571    318386         6         6         6
			Fopt_PL      78.821      13.366      28.241         6        75       245       182         6        84        20       184         6         6         6
		   Fopt_CPL      63.882       6.556      12.169         6        19       245       100         6        18        20        84         6         6         6
		   Fopt_CPR      43.342       6.041       7.007         6        19       245       100         6        16        20        69         6         6         6
		  Fopt_CPLR      36.409       6.831       7.346         6        19       245       100         6        16        20        69         6         6         6
		 */
		DatasetParam param = new DatasetParam("WIKI", "100", "1000", "5", "1.0");
//		DatasetParam param = new DatasetParam("WIKI-DOC", "20", "1000", "5", "1.0");
		String argsTmpl = "-data %s -alg PrefixSearch -nt %s -nr %s -ql %s -lr %s -param theta:0.6,filter:%s";
		String outputTmpl = "%15s%12.3f%12.3f%12.3f%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d%10d\n";
		StringBuilder strbld = new StringBuilder(String.format("%15s%12s%12s%12s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s\n", "FilterOption", 
				"T_Total", "T_Qside", "T_Tside", 
				"N_Q_Ret", "N_T_Ret", "L_Q_Ret", "L_T_Ret", "N_Q_Val", "N_T_Val", "L_Q_Val", "L_T_Val", "N_Res", "N_Q_Res", "N_T_Res"));
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		for ( AlgorithmFactory.FilterOptionLabel opt : FilterOptionLabel.values() ) {
//			if ( opt == FilterOptionLabel.Fopt_None ) continue;
			String[] args = String.format(argsTmpl, param.name, param.size, param.nr, param.qlen, param.lenRatio, opt).split(" ");
			AbstractSearch alg = AlgorithmFactory.createInstance(new InputArgument(args));
			alg.run(dataset);
			strbld.append(String.format(outputTmpl, opt, 
					Double.parseDouble(alg.getStat(Stat.Time_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_QS_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_TS_Total)), 
					Integer.parseInt(alg.getStat(Stat.Num_QS_Retrieved)), 
					Integer.parseInt(alg.getStat(Stat.Num_TS_Retrieved)),
					Integer.parseInt(alg.getStat(Stat.Len_QS_Retrieved)), 
					Integer.parseInt(alg.getStat(Stat.Len_TS_Retrieved)),
					Integer.parseInt(alg.getStat(Stat.Num_QS_Verified)), 
					Integer.parseInt(alg.getStat(Stat.Num_TS_Verified)),
					Integer.parseInt(alg.getStat(Stat.Len_QS_Verified)), 
					Integer.parseInt(alg.getStat(Stat.Len_TS_Verified)),
					Integer.parseInt(alg.getStat(Stat.Num_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_QS_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_TS_Result))
					));
		}
		System.out.println(strbld.toString());
	}
	
	@Ignore
	public void testLengthFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = DatasetFactory.createInstanceByName(name, size);

		String[] results = new String[4];
		int i = 0;
		for ( boolean bLF: new boolean[]{false, true} ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, bLF, false, IndexChoice.Naive);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_Total);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_Total);
			results[i++] = String.format("%s\t%s\t%s\t%s", bLF, time_0, time_1, time_2);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "lf_query", "lf_text", Stat.Time_Total, Stat.Time_QS_Total, Stat.Time_TS_Total));
		for ( String result : results ) {
			System.out.println(result);
		}
	}
	
	@Ignore
	public void testIndexFilter() throws IOException {
		String size = "100";
		double theta = 0.7;
		Dataset dataset = DatasetFactory.createInstanceByName(name, size);

		String[] results = new String[4];
		int i = 0;
		for ( IndexChoice indexChoice : IndexChoice.values() ) {
			AbstractSearch prefixSearch = new PrefixSearch(theta, true, true, indexChoice);
			prefixSearch.run(dataset);
			String time_0 = prefixSearch.getStatContainer().getStat(Stat.Time_Total);
			String time_1 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_Total);
			String time_2 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_Total);
			String time_5 = prefixSearch.getStatContainer().getStat(Stat.Time_QS_IndexFilter);
			String time_6 = prefixSearch.getStatContainer().getStat(Stat.Time_TS_IndexFilter);
			results[i++] = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", indexChoice.toString(), time_0, time_1, time_2, time_5, time_6);
		}
		System.out.println(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", "idxFilter_query", "idxFilter_text", Stat.Time_Total, Stat.Time_QS_Total, Stat.Time_TS_Total, Stat.Time_QS_IndexFilter, Stat.Time_TS_IndexFilter));
		for ( String result : results ) {
			System.out.println(result);
		}
	}

	public void test( String name, String size, String nr, String ql, double theta ) throws IOException {
		DatasetParam param = new DatasetParam(name, size, nr, ql, null);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		ExactNaiveSearch naiveSearch = new ExactNaiveSearch(theta);
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.CountPosition);
		
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
	
	@Ignore
	public void testIndexImplComparison() throws IOException {
		String name = "SPROT_long";
		String size = "102";
		double theta = 0.6;
		Dataset dataset = DatasetFactory.createInstanceByName(name, size);
		
		AbstractSearch prefixSearch = null;
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.Count);
		prefixSearch.run(dataset);
		String num_qs0 = prefixSearch.getStatContainer().getStat(Stat.Num_QS_Result);
		String num_ts0 =prefixSearch.getStatContainer().getStat(Stat.Num_TS_Result);
		prefixSearch = new ExactPrefixSearch(theta, true, true, IndexChoice.CountPosition);
		prefixSearch.run(dataset);
		String num_qs1 = prefixSearch.getStatContainer().getStat(Stat.Num_QS_Result);
		String num_ts1 =prefixSearch.getStatContainer().getStat(Stat.Num_TS_Result);
		assertTrue(num_qs0.equals(num_qs1));
		assertTrue(num_ts0.equals(num_ts1));
	}

	@Test
	public void testVaryLenRatio() {
		/*	
		    WIKI_n10000_r3162_q3, theta=0.6
		    0.2	942.045	903	901	903
			0.4	828.984	2547	2539	2547
			0.6	1275.105	4129	4109	4129
			0.8	1520.456	5617	5588	5617
			1.0	1799.332	7129	7090	7129

			WIKI_n10000_r3162_q3, theta=1.0
			0.2	561.684	12	12	12
			0.4	691.665	46	46	46
			0.6	928.745	89	87	89
			0.8	936.643	123	121	123
			1.0	1107.630	195	193	195

		 */
		Log.disable();
		for ( String lenRatio : new String[] {"0.2", "0.4", "0.6", "0.8", "1.0"}) {
			Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "3162", "3", lenRatio);
			double theta = 1.0;
			AbstractSearch alg1 = new PositionPrefixSearch(theta, true, true, IndexChoice.CountPosition);
			alg1.run(dataset);
			System.out.println(lenRatio
					+"\t"+alg1.getStatContainer().getStat(Stat.Time_Total)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_Result)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_QS_Result)
					+"\t"+alg1.getStatContainer().getStat(Stat.Num_TS_Result));
		}
	}
	
	@Test
	public void testVaryRecordPoolSize() throws IOException {
		/*
		AMAZON_n300000_r107836_q5_l1.0, theta=0.6
		RecordPool.size     T_Total        T_QS        T_TS    T_QS_Rec    T_TS_Rec    N_QS_F    N_TS_F     N_Res  N_QS_Res  N_TS_Res
			 	0.0e+00   81046.475   33125.389   38552.535   25966.848   19879.682   1835313    887680     57452     39434     55858
				1.0e+03   81596.689   33555.919   39254.822   26783.134   20284.497   1835313    887680     57452     39434     55858
				1.0e+04   81527.797   33960.685   37508.089   27058.285   20256.111   1835301    887665     57452     39434     55858
				1.0e+05   83080.164   34524.922   38113.782   27633.903   20539.307   1834312    886424     57452     39434     55858
				1.0e+06   89892.782   39711.307   40103.000   32825.453   19554.414   1679517    773187     57452     39434     55858
				1.0e+07   47089.132   14832.757   23685.047    3983.474    5432.434    235646    219655     57452     39434     55858
				1.0e+08   53995.918   11677.661   28738.215    4183.679    5969.517    235646    219655     57452     39434     55858

		AMAZON_n100000_r107836_q5_l1.0, theta=0.6
		RecordPool.size     T_Total        T_QS        T_TS    T_QS_Rec    T_TS_Rec    N_QS_F    N_TS_F     N_Res  N_QS_Res  N_TS_Res
				0.0e+00   26273.659    9913.431   12318.754    7600.578    6214.313    610108    295078     22492     13255     22364
				1.0e+03   24737.995    9606.849   11932.784    7572.674    6222.871    610108    295077     22492     13255     22364
				1.0e+04   24804.512    9555.723   11969.028    7572.490    6222.015    610074    295045     22492     13255     22364
				1.0e+05   24732.035    9723.573   11896.971    7739.803    6010.949    604249    290393     22492     13255     22364
				1.0e+06   20303.906    5913.986    9815.224    3708.202    3584.801    265364    147716     22492     13255     22364
				1.0e+07   16423.526    3030.785   10372.152    1147.806    1741.924     78648     73136     22492     13255     22364
				1.0e+08   17128.892    5822.520    8263.366    1237.910    1711.962     78648     73136     22492     13255     22364

		 */
		DatasetParam param = new DatasetParam("AMAZON", "300000", "107836", "5", "1.0");
		double theta = 0.6;
		String outputTmpl = "%16.1e%12.3f%12.3f%12.3f%12.3f%12.3f%10d%10d%10d%10d%10d\n";
		StringBuilder strbld = new StringBuilder(String.format("%16s%12s%12s%12s%12s%12s%10s%10s%10s%10s%10s\n", "RecordPool.size", "T_Total", "T_QS", "T_TS", "T_QS_Rec", "T_TS_Rec", "N_QS_F", "N_TS_F", "N_Res", "N_QS_Res", "N_TS_Res"));
		for ( double size : new double[] {0, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8} ) {
			RecordPool.BUFFER_SIZE = (int)size;
			Dataset dataset = DatasetFactory.createInstanceByName(param);
			AbstractSearch alg = new PositionPrefixSearch(theta, false, false, IndexChoice.CountPosition);
			alg.run(dataset);
			strbld.append(String.format(outputTmpl, 
					size, 
					Double.parseDouble(alg.getStat(Stat.Time_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_QS_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_TS_Total)), 
					Double.parseDouble(alg.getStat("Time_QS_IndexFilter.getRecord")), 
					Double.parseDouble(alg.getStat("Time_TS_IndexFilter.getRecord")),
					Integer.parseInt(alg.getStat("Num_QS_RecordFault")), 
					Integer.parseInt(alg.getStat("Num_TS_RecordFault")), 
					Integer.parseInt(alg.getStat(Stat.Num_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_QS_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_TS_Result))
			));
		}
		System.out.println(strbld.toString());
	}

	@Test
	public void testVaryNAR() throws IOException {
		/*
		AMAZON_n10000_r107836_q5_l1.0, theta=0.6
		 nar     T_Total        T_QS        T_TS     N_rec     N_Res  N_QS_Res  N_TS_Res    N_QS_V    N_TS_V
		  -1    3110.945     994.145    1494.107     10000      1380      2305      2317     17020    183744
		  30    1369.897     422.495     462.091     10000       682       905       906      6563     48241
		  20     641.764     187.819     193.601     10000       555       661       664      4911     31433
		  10     428.004     125.606     101.910     10000       455       492       492      3595     14108
		   5     323.115      87.505      62.148     10000       359       377       377      2474      6596
		   2     219.300      53.902      33.535     10000       273       276       276      1781      2288
		   1     201.476      48.998      29.174     10000       277       280       280      1770      1464
		   0     196.080      41.334      23.407     10000       266       266       266      1632       802 
		 */
		int[] narArr = {-1, 30, 20, 10, 5, 2, 1, 0};
		StringBuilder strbld = new StringBuilder(String.format("%4s%12s%12s%12s%10s%10s%10s%10s%10s%10s\n", "nar", "T_Total", "T_QS", "T_TS", "N_rec", "N_Res", "N_QS_Res", "N_TS_Res", "N_QS_V", "N_TS_V"));
		for ( int nar : narArr ) {
			DatasetParam param = new DatasetParam("AMAZON", "10000", "107836", "5", "1.0", ""+nar);
			double theta = 0.6;
			Dataset dataset = DatasetFactory.createInstanceByName(param);
			AbstractSearch alg = new PositionPrefixSearch(theta, false, false, IndexChoice.CountPosition);
			alg.run(dataset);
			strbld.append(String.format("%4d%12.3f%12.3f%12.3f%10d%10d%10d%10d%10d%10d\n",
					nar, 
					Double.parseDouble(alg.getStat(Stat.Time_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_QS_Total)), 
					Double.parseDouble(alg.getStat(Stat.Time_TS_Total)), 
					Integer.parseInt(alg.getStat(Stat.Dataset_numIndexed)), 
					Integer.parseInt(alg.getStat(Stat.Num_QS_Result)), 
					Integer.parseInt(alg.getStat(Stat.Num_TS_Result)),
					Integer.parseInt(alg.getStat(Stat.Num_Result)), 
					Integer.parseInt(alg.getStat(Stat.Len_QS_Verified)), 
					Integer.parseInt(alg.getStat(Stat.Len_TS_Verified))
			));
		}
		System.out.println(strbld.toString());
	}
}

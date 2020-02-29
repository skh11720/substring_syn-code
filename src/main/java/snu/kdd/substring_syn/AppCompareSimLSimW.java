package snu.kdd.substring_syn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.variants.PerQueryPositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.algorithm.validator.NaiveWindowBasedValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class AppCompareSimLSimW {

	static final double EPS = 1e-5;
	static PrintWriter pw = null;
	static NaiveValidator val0 = new NaiveValidator(0, null);
	static NaiveWindowBasedValidator val1 = new NaiveWindowBasedValidator(0, null);
	static int nAppRuleMax = 10;
	
	private static void initialize() {
		FileUtils.listFiles(new File("./tmp"), null, false).stream().forEach(f -> f.delete());
	}

    public static void main( String[] args ) throws ParseException, IOException, InterruptedException, ExecutionException {
    	initialize();

		Options argOptions = new Options();
		argOptions.addOption("data", true, "");
		argOptions.addOption("nt", true, "");
		argOptions.addOption("nq", true, "");
		argOptions.addOption("ql", true, "");
		argOptions.addOption("nr", true, "");
		argOptions.addOption("theta", true, "");
		argOptions.addOption("nAppRuleMax", true, "");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(argOptions, args, false);

		String dataName = cmd.getOptionValue("data");
		String size = cmd.getOptionValue("nt");
		String nq = cmd.getOptionValue("nq");
		String qlen = cmd.getOptionValue("ql");
		String nr = cmd.getOptionValue("nr");
		double theta = Double.parseDouble(cmd.getOptionValue("theta"));
		nAppRuleMax = Integer.parseInt(cmd.getOptionValue("nAppRuleMax"));

		String outputName = String.format("output/AppCompareSimLSimW_%s_%s_%s_%s_%s_%.1f.txt", dataName, size, nq, qlen, nr, theta);
		pw = new PrintWriter(new BufferedWriter(new FileWriter(outputName, false)));
		DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
//		if (theta < EPS ) runNaive(dataset, nq);
//		else 
		runAlg(dataset, nq, theta);
		pw.close();
    }

//    public static void runNaive(Dataset dataset, String nq) throws InterruptedException, ExecutionException {
//		for ( Record rec : dataset.getIndexedList() ) {
//			Log.log.info("rec.id=%d", rec.getID());
//			rec.preprocessAll();
//			for ( Record query : dataset.getSearchedList() ) {
//				verifyPair(query, rec);
//				if ( query.getID()-1 >= Integer.parseInt(nq) ) break;
//			}
//			pw.flush();
//		}
//    }
    
    public static void runAlg(Dataset dataset, String nq, double theta) throws InterruptedException, ExecutionException {
    	Log.log.info(dataset.name);
		PerQueryPositionPrefixSearch alg = new PerQueryPositionPrefixSearch(dataset, theta, true, false, IndexChoice.CountPosition);
		IntArrayList nL_List = new IntArrayList();
		IntArrayList nW_List = new IntArrayList();
		int nQ = 0;
		for ( Record query : dataset.getSearchedList() ) {
			int nL = 0;
			int nW = 0;
			Set<IntPair> rslt = alg.searchTextSideGivenQuery(query);
			for ( IntPair pair : rslt ) {
				int rid = pair.i2;
				Record rec = dataset.getRecord(rid);
				rec.preprocessAll();
				if ( rec.getNumApplicableRules() > nAppRuleMax ) continue;
				
				nL += 1;
				double sim0 = val0.simTextSide(query, rec);
				double sim1 = val1.simTextSide(query, rec);

				if( Math.abs(sim0-sim1) < EPS ) {
					nW += 1;
					pw.printf("E\t%d %d %.6f\n", query.getID(), rec.getID(), sim0);
				}
				else pw.printf("N\t%d %d %.6f %.6f\n", query.getID(), rec.getID(), sim0, sim1);
			}
			if (nL > 0) {
				nQ += 1;
				nL_List.add(nL);
				nW_List.add(nW);
			}
			if ((query.getID()+1)%1000 == 0) {
				Log.log.info("num processed queries: %d", query.getID()+1);
				pw.flush();
			}
			if ( query.getID()-1 >= Integer.parseInt(nq) ) break;
		}
		pw.flush();
		int nLsum = nL_List.stream().mapToInt(Integer::intValue).sum();
		int nWsum = nW_List.stream().mapToInt(Integer::intValue).sum();
		Log.log.info("OUTPUT: nQ=%d\tnL_sum=%d\tnW_sum=%d\tnL_sum/nQ=%.3f\t(nL-nW)/nQ=%.3f", nQ, nLsum, nWsum, 1.0*nLsum/nQ, 1.0*(nLsum-nWsum)/nQ);
    }
    
//    private static void verifyPair(Record query, Record rec) throws InterruptedException, ExecutionException {
//		double sim0 = val0.simTextSide(query, rec);
//		double sim1 = val1.simTextSide(query, rec);
//
//		if( Math.abs(sim0-sim1) < EPS ) pw.printf("E\t%d %d %.6f\n", query.getID(), rec.getID(), sim0);
//		else pw.printf("N\t%d %d %.6f %.6f\n", query.getID(), rec.getID(), sim0, sim1);
//    }
    
//    static class Task0 implements Callable<Double> {
//    	
//    	Record query, rec;
//    	
//    	public Task0(Record query, Record rec) {
//    		this.query = query;
//    		this.rec = rec;
//		}
//
//		@Override
//		public Double call() throws Exception {
//			return val0.simTextSide(query, rec);
//		}
//    }
//
//	static class Task1 implements Callable<Double> {
//    	
//    	Record query, rec;
//    	
//    	public Task1(Record query, Record rec) {
//    		this.query = query;
//    		this.rec = rec;
//		}
//
//		@Override
//		public Double call() throws Exception {
//			return val1.simTextSide(query, rec);
//		}
//    }
}

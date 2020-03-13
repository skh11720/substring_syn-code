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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.variants.PerQueryExactPositionPrefixSearch;
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
	static double theta;
	
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
		argOptions.addOption("nar", true, "");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(argOptions, args, false);

		String dataName = cmd.getOptionValue("data");
		String size = cmd.getOptionValue("nt");
		String nq = cmd.getOptionValue("nq");
		String qlen = cmd.getOptionValue("ql");
		String nr = cmd.getOptionValue("nr");
		String nar = cmd.getOptionValue("nar");
		theta = Double.parseDouble(cmd.getOptionValue("theta"));

		String outputName = String.format("output/AppCompareSimLSimW.txt");
		pw = new PrintWriter(new BufferedWriter(new FileWriter(outputName, true)));
		DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0", nar);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		DatasetContainer datasetContainer = new DatasetContainer(dataset);
//		if (theta < EPS ) runNaive(dataset, nq);
//		else 
		runAlg(datasetContainer, nq, theta);
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
    
    public static void runAlg(DatasetContainer datasetContainer, String nq, double theta) throws InterruptedException, ExecutionException {
    	Dataset dataset = datasetContainer.dataset;
    	Log.log.info(dataset.name);
		PerQueryExactPositionPrefixSearch alg = new PerQueryExactPositionPrefixSearch(dataset, theta, true, false, IndexChoice.CountPosition);
		IntArrayList nL_List = new IntArrayList();
		IntArrayList nW_List = new IntArrayList();
		int nQ = 0;
		for ( Record query : dataset.getSearchedList() ) {
			int nL = 0;
			int nW = 0;
			Set<IntPair> rslt = alg.searchTextSideGivenQuery(query);
			for ( IntPair pair : rslt ) {
				nL += 1;
				int idx = pair.i2;
				if ( checkSimW(datasetContainer, query, idx) ) nW += 1;
			}
			if (nL > 0) {
				nQ += 1;
				nL_List.add(nL);
				nW_List.add(nW);
			}
			if ((query.getIdx()+1)%1000 == 0) {
				Log.log.info("num processed queries: %d", query.getIdx()+1);
//				pw.flush();
			}
			if ( query.getIdx()-1 >= Integer.parseInt(nq) ) break;
		}
		int nLsum = nL_List.stream().mapToInt(Integer::intValue).sum();
		int nWsum = nW_List.stream().mapToInt(Integer::intValue).sum();
		String summary = String.format("dataset=%s\tnq=%s\ttheta=%.1f\tnarMax=%s\tnQ=%d\tnL_sum=%d\tnW_sum=%d\tnL_sum/nQ=%.3f\t(nLsum-nWsum)/nQ=%.3f\t(nLsum-nWsum)/nLsum=%.3f", dataset.name, nq, theta, dataset.param.nar, nQ, nLsum, nWsum, 1.0*nLsum/nQ, 1.0*(nLsum-nWsum)/nQ, 1.0*(nLsum-nWsum)/nLsum);
		Log.log.info(summary);
		pw.println(summary);
		pw.flush();
    }
    
    private static boolean checkSimW(DatasetContainer datasetContainer, Record query, int idx) {
    	if ( datasetContainer.dataset.isDocInput() ) 
    		return checkSimWDoc(datasetContainer, query, idx);
    	else
    		return checkSimWSnt(datasetContainer, query, idx);
    }
    
    private static boolean checkSimWSnt(DatasetContainer datasetContainer, Record query, int ridx) {
		Record rec = datasetContainer.dataset.getRecord(ridx);
		rec.preprocessAll();
		double sim1 = val1.simTextSide(query, rec);
		return (sim1 >= theta-EPS);
    }
    
    private static boolean checkSimWDoc(DatasetContainer datasetContainer, Record query, int didx) {
    	for ( int ridx : datasetContainer.did2ridListMap.get(didx) ) {
    		Record rec = datasetContainer.dataset.getRecord(ridx);
    		rec.preprocessAll();
    		double sim1 = val1.simTextSide(query, rec);
    		if( sim1 >= theta-EPS ) return true;
    	}
    	return false;
    }
    
    
    
    private static class DatasetContainer {
    	final Dataset dataset;
    	final Int2ObjectMap<IntArrayList> did2ridListMap;
    	
    	public DatasetContainer(Dataset dataset) {
    		this.dataset = dataset;
    		if ( dataset.isDocInput() ) {
    			did2ridListMap = new Int2ObjectOpenHashMap<>();
    			for ( Record rec : dataset.getIndexedList() ) {
    				int did = dataset.getRid2idpairMap().get(rec.getIdx()).i1;
    				if ( did2ridListMap.get(did) == null ) did2ridListMap.put(did, new IntArrayList());
    				did2ridListMap.get(did).add(rec.getIdx());
    			}
    		}
    		else did2ridListMap = null;
		}
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

package snu.kdd.substring_syn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;


public class AppValidationDifference {

	static final double EPS = 1e-10;
	static PrintWriter pw = null;
	static double theta;
	static int maxlen;
	
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
		argOptions.addOption("maxlen", true, "");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(argOptions, args, false);

		String dataName = cmd.getOptionValue("data");
		String size = cmd.getOptionValue("nt");
		String nq = cmd.getOptionValue("nq");
		String qlen = cmd.getOptionValue("ql");
		String nr = cmd.getOptionValue("nr");
		String nar = cmd.getOptionValue("nar");
		maxlen = Integer.parseInt(cmd.getOptionValue("maxlen"));
		theta = Double.parseDouble(cmd.getOptionValue("theta"));

		String outputName = String.format("output/AppValidationDifference.txt");
		pw = new PrintWriter(new BufferedWriter(new FileWriter(outputName, true)));
		DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0", nar);
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		DatasetContainer datasetContainer = new DatasetContainer(dataset);
		runAlg(datasetContainer, nq, theta);
		pw.close();
    }

    public static void runAlg(DatasetContainer datasetContainer, String nq, double theta) throws InterruptedException, ExecutionException {
    	Dataset dataset = datasetContainer.dataset;
    	StatContainer statContainer = new StatContainer();
    	Log.log.info(dataset.name);
		NaiveValidator val0 = new NaiveValidator(theta, statContainer);
		GreedyValidator val1 = new GreedyValidator(theta, statContainer);
		int n = 0;
		double diffsumQ = 0;
		double diffsumT = 0;
		double diffsum = 0;
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
				rec.preprocessApplicableRules();
				rec.preprocessSuffixApplicableRules();
				double simQ0 = val0.simQuerySide(query, rec);
				double simT0 = val0.simTextSide(query, rec);
				double sim0 = Math.max(simQ0, simT0);
				double simQ1 = val1.simQuerySide(query, rec);
				double simT1 = val1.simTextSide(query, rec);
				double sim1 = Math.max(simQ1, simT1);
				assert simQ0 + EPS >= simQ1;
				assert simT0 + EPS >= simT1;
				assert sim0 + EPS >= sim1;
				diffsumQ += (simQ0 - simQ1);
				diffsumT += (simT0 - simT1);
				diffsum += (sim0 - sim1);
				n += 1;
				Log.log.trace("sim: %.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f", simQ0, simQ1, (simQ0-simQ1), simT0, simT1, (simT0-simT1), sim0, sim1, (sim0-sim1));
			}
		}
		String summary = String.format("dataset=%s\tnq=%s\ttheta=%.1f\tnarMax=%s\tmaxlen=%d\tavgdiff_Q=%.6f\tavgdiff_T=%.6f\tavgdiff=%.6f", dataset.name, nq, theta, dataset.param.nar, maxlen, diffsumQ/n, diffsumT/n, diffsum/n);
		Log.log.info(summary);
		pw.println(summary);
		pw.flush();
    }
    
    
    
    private static class DatasetContainer {
    	final Dataset dataset;
    	final Int2ObjectMap<IntArrayList> did2ridxListMap;
    	
    	public DatasetContainer(Dataset dataset) {
    		this.dataset = dataset;
    		if ( dataset.isDocInput() ) {
    			did2ridxListMap = new Int2ObjectOpenHashMap<>();
    			for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
    				int did = dataset.getRid2idpairMap().get(rec.getIdx()).i1;
    				if ( did2ridxListMap.get(did) == null ) did2ridxListMap.put(did, new IntArrayList());
    				did2ridxListMap.get(did).add(rec.getIdx());
    			}
    		}
    		else did2ridxListMap = null;
		}
    }
}

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
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.SortedWindowExpander;


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

	protected static double searchRecordQuerySide( Record query, RecordInterface rec, GreedyValidator val ) {
		double sim = 0;
		int min = (int)Math.max(1, Math.ceil(theta*query.getMinTransLength()));
		int max = (int)Math.min(1.0*query.getMaxTransLength()/theta, rec.size());
		IntRange wRange = new IntRange(min, max);
		for ( int widx=0; widx<rec.size(); ++widx ) {
			SortedWindowExpander witer = new SortedWindowExpander(rec, widx, theta);
			while ( witer.hasNext() ) {
				Subrecord window = witer.next();
				if ( window.size() > wRange.max ) break;
				if ( window.size() < wRange.min ) continue;
				sim = Math.max(sim, val.simQuerySide(query, window));
			}
		}
		return sim;
	}

	protected static double searchRecordTextSide( Record query, TransformableRecordInterface rec, GreedyValidator val ) {
		double sim = 0;
		double modifiedTheta = Util.getModifiedTheta(query, rec, theta);
		for ( int widx=0; widx<rec.size(); ++widx ) {
			TransLenLazyCalculator transLenCalculator = new TransLenLazyCalculator(null, rec, widx, rec.size()-widx, modifiedTheta);
			for ( int w=1; w<=rec.size()-widx; ++w ) {
				if ( transLenCalculator.getLFLB(widx+w-1) > query.size() ) break;
				Subrecord window = new Subrecord(rec, widx, widx+w);
				sim = Math.max(sim, val.simTextSide(query, window));
			}
		}
		return sim;
	}

    public static void runAlg(DatasetContainer datasetContainer, String nq, double theta) throws InterruptedException, ExecutionException {
    	Dataset dataset = datasetContainer.dataset;
    	StatContainer statContainer = new StatContainer();
    	StatContainer.global = new StatContainer();
    	Log.log.info(dataset.name);
		NaiveValidator val0 = new NaiveValidator(theta, statContainer);
		GreedyValidator val1 = new GreedyValidator(theta, statContainer);
		int n0 = 0;
		int n1 = 0;
		int nQ0 = 0;
		int nQ1 = 0;
		int nT0 = 0;
		int nT1 = 0;
		double diffsumQ = 0;
		double diffsumT = 0;
		double diffsum = 0;
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();

			for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
				double simQ0 = val0.simQuerySide(query, rec);
				double simQ1 = searchRecordQuerySide(query, rec, val1);
					
				if ( simQ0 >= theta ) {
					diffsumQ += (simQ0 - simQ1);
					nQ0 += 1;
					if ( simQ1 >= theta ) nQ1 += 1;
					Log.log.trace("simQ: %.6f\t%.6f\t%.6f", simQ0, simQ1, (simQ0-simQ1));
				}
				
				double simT0 = val0.simTextSide(query, rec);
				double simT1 = searchRecordTextSide(query, rec, val1);

				if ( simT0 >= theta ) {
					diffsumT += (simT0 - simT1);
					nT0 += 1;
					if ( simT1 >= theta ) nT1 += 1;
					Log.log.trace("simT: %.6f\t%.6f\t%.6f", simT0, simT1, (simT0-simT1));
				}

				double sim0 = Math.max(simQ0, simT0);
				double sim1 = Math.max(simQ1, simT1);
				assert sim0 + EPS >= sim1;
				if ( sim0 >= theta ) {
					diffsum += (sim0 - sim1);
					n0 += 1;
					if ( sim1 >= theta ) n1 += 1;
					Log.log.trace("sim: %.6f\t%.6f\t%.6f", sim0, sim1, (sim0-sim1));
				}
			}
		}
		String summary = "";
		summary += String.format("dataset=%s\tnq=%s\ttheta=%.1f\tnarMax=%s\tmaxlen=%d", dataset.name, nq, theta, dataset.param.nar, maxlen);
		summary += String.format("\tavgdiff_Q=%.6f\tnQ0=%d\tnQ1=%d\tnQ1/nQ0=%.6f", diffsumQ/nQ0, nQ0, nQ1, 1.0*nQ1/nQ0);
		summary += String.format("\tavgdiff_T=%.6f\tnT0=%d\tnT1=%d\tnT1/nT0=%.6f", diffsumT/nT0, nT0, nT1, 1.0*nT1/nT0);
		summary += String.format("\tavgdiff=%.6f\tn0=%d\tn1=%d\tn1/n0=%.6f", diffsum/n0, n0, n1, 1.0*n1/n0);
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

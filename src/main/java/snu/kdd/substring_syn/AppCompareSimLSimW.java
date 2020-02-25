package snu.kdd.substring_syn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.ExactPositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.ExactSimWPositionPrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;

public class AppCompareSimLSimW {
	
	private static void initialize() {
		FileUtils.listFiles(new File("./tmp"), null, false).stream().forEach(f -> f.delete());
	}

    public static void main( String[] args ) throws ParseException, IOException {
    	initialize();

		Options argOptions = new Options();
		argOptions.addOption("data", true, "");
		argOptions.addOption("nt", true, "");
		argOptions.addOption("ql", true, "");
		argOptions.addOption("nr", true, "");
		argOptions.addOption("theta", true, "");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(argOptions, args, false);

		String dataName = cmd.getOptionValue("data");
		String size = cmd.getOptionValue("nt");
		String qlen = cmd.getOptionValue("ql");
		String nr = cmd.getOptionValue("nr");
		double theta = Double.parseDouble(cmd.getOptionValue("theta"));

		String outputName = String.format("output/AppCompareSimLSimW_%s_%s_%s_%s_%.1f.txt", dataName, size, qlen, nr, theta);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputName, false)));

		DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		ExactPositionPrefixSearch alg0 = new ExactPositionPrefixSearch(theta, true, false, IndexChoice.CountPosition);
		ExactSimWPositionPrefixSearch alg1 = new ExactSimWPositionPrefixSearch(theta, true, false, IndexChoice.CountPosition);
		alg0.run(dataset);
		alg1.run(dataset);
		Set<IntPair> rslt0 = alg0.getResultTextSide();
		Set<IntPair> rslt1 = alg1.getResultTextSide();
		rslt0.removeAll(rslt1);
		pw.append(String.format("E\t%s_%s_%s_%.1f\t%d\n", dataName, qlen, nr, theta, rslt1.size()));
		for ( IntPair pair : rslt0 ) {
			System.out.printf("%8s%4s%8s%8.1f%8d%8d\n", dataName, qlen, nr, theta, pair.i1, pair.i2);
			pw.append(String.format("N_%s_%s_%s_%.1f\t%d\t%d\n", dataName, qlen, nr, theta, pair.i1, pair.i2));
		}
		pw.flush();
		pw.close();
    }
}

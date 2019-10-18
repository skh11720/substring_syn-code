package snu.kdd.substring_syn;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.utils.Util;

public class App {
	
	private static Options argOptions;
	
	static {
		argOptions = new Options();
		argOptions.addOption("data", true, "");
		argOptions.addOption("nq", true, "");
		argOptions.addOption("nt", true, "");
		argOptions.addOption("nr", true, "");
		argOptions.addOption("ql", true, "");
		argOptions.addOption("alg", true, "");
		argOptions.addOption("param", true, "");
	}

	private static CommandLine parseInput( String args[] ) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args, false );
		Util.printArgsError( cmd );
		return cmd;
	}

    public static void main( String[] args ) throws ParseException, IOException {
    	CommandLine cmd = parseInput(args);
    	Dataset dataset = Dataset.createInstance(cmd);
    	AbstractSearch alg = AlgorithmFactory.createInstance(cmd);
    	alg.run(dataset);
    	alg.getStatContainer().outputJson();
    }
}

package snu.kdd.substring_syn.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class InputArgument {

	public static Options argOptions;

	static {
		argOptions = new Options();
		argOptions.addOption("data", true, "");
		argOptions.addOption("nq", true, "");
		argOptions.addOption("nt", true, "");
		argOptions.addOption("nr", true, "");
		argOptions.addOption("ql", true, "");
		argOptions.addOption("lr", true, "");
		argOptions.addOption("alg", true, "");
		argOptions.addOption("param", true, "");
		argOptions.addOption("pool", true, "");
	}
	
	private final CommandLine cmd;

	public InputArgument(String args[]) {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( argOptions, args, false );
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.cmd = cmd;
		Util.printArgsError(cmd);
	}

	public String getOptionValue(String key) {
		String value = cmd.getOptionValue(key);
		if ( value == null ) throw new RuntimeException("Invalid input argument: "+key+" = "+value);
		return value;
	}

}

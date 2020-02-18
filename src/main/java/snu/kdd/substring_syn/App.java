package snu.kdd.substring_syn;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.utils.InputArgument;

public class App {
	
	private static void initialize() {
		FileUtils.listFiles(new File("./tmp"), null, false).stream().forEach(f -> f.delete());
	}

    public static void main( String[] args ) throws ParseException, IOException {
    	initialize();
    	InputArgument arg = new InputArgument(args);
    	Dataset dataset = DatasetFactory.createInstance(arg);
    	AbstractSearch alg = AlgorithmFactory.createInstance(arg);
    	alg.run(dataset);
    	alg.getStatContainer().outputJson();
    }
}

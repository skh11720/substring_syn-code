package snu.kdd.substring_syn.data;

import java.io.IOException;

import snu.kdd.pkwise.PkwiseTokenIndexBuilder;
import snu.kdd.pkwise.TransWindowDataset;
import snu.kdd.pkwise.WindowDataset;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.AlgorithmName;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.InputArgument;

public class DatasetFactory {

	public static Dataset createInstance( InputArgument arg ) throws IOException {
		DatasetParam param = new DatasetParam(arg);
		AlgorithmName algName = AlgorithmName.valueOf( arg.getOptionValue("alg") );
		if ( algName == AlgorithmName.PkwiseSearch || algName == AlgorithmName.PkwiseNaiveSearch )
			return createWindowInstanceByName(param);
		if ( algName == AlgorithmName.PkwiseSynSearch ) {
			String paramStr = arg.getOptionValue("param");
			String theta = paramStr.split(",")[0].split(":")[1];
			return createTransWindowInstanceByName(param, theta);
		}
		else
			return createInstanceByName(param);
	}
	
	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		return createInstanceByName(new DatasetParam(name, size, null, null, null));
	}

	public static Dataset createInstanceByName(DatasetParam param) throws IOException {
		DiskBasedDataset dataset = new DiskBasedDataset(param);
		dataset.createRuleSet();
		dataset.addStat();
		dataset.statContainer.finalize();
		return dataset;
	}
	
	public static WindowDataset createWindowInstanceByName(DatasetParam param) throws IOException {
		WindowDataset dataset = new WindowDataset(param);
		Record.tokenIndex = PkwiseTokenIndexBuilder.build(dataset, Integer.parseInt(param.qlen));
		dataset.buildRecordStore();
		dataset.createRuleSet();
		dataset.addStat();
		dataset.statContainer.finalize();
//		dataset.ruleSet.writeToFile();
		return dataset;
	}

	public static TransWindowDataset createTransWindowInstanceByName(DatasetParam param, String theta) throws IOException {
		TransWindowDataset dataset = new TransWindowDataset(param, theta);
		Record.tokenIndex = PkwiseTokenIndexBuilder.build(dataset, Integer.parseInt(param.qlen));
		dataset.buildRecordStore();
		dataset.createRuleSet();
		dataset.buildIntQGramStore();
		dataset.addStat();
		dataset.statContainer.finalize();
//		dataset.ruleSet.writeToFile();
		return dataset;
	}
}

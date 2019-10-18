package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.pkwise.PkwiseTokenOrder;
import snu.kdd.pkwise.TransWindowDataset;
import snu.kdd.pkwise.WindowDataset;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.AlgorithmName;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class Dataset {
	
	public final String name;
	public final String searchedPath;
	public final String indexedPath;
	public final String rulePath;
	public final String outputPath;
	public final StatContainer statContainer;

	public Ruleset ruleSet;

	public static Dataset createInstance( CommandLine cmd ) throws IOException {
		String name = getOptionValue(cmd, "data");
		String size = getOptionValue(cmd, "nt");
		String nr = getOptionValue(cmd, "nr");
		String qlen = getOptionValue(cmd, "ql");
		AlgorithmName algName = AlgorithmName.valueOf( cmd.getOptionValue("alg") );
		if ( algName == AlgorithmName.PkwiseSearch || algName == AlgorithmName.PkwiseNaiveSearch )
			return createWindowInstanceByName(name, size, nr, qlen);
		if ( algName == AlgorithmName.PkwiseSynSearch ) {
			String theta = getOptionValue(cmd, "theta");
			return createTransWindowInstanceByName(name, size, nr, qlen, theta);
		}
		else
			return createInstanceByName(name, size, nr, qlen);
	}
	
	private static String getOptionValue( CommandLine cmd, String key ) {
		String value = cmd.getOptionValue(key);
		if ( value == null ) throw new RuntimeException("Invalid input argument: "+key+" = "+value);
		return value;
	}

	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		return createInstanceByName(name, size, null, null);
	}

	public static Dataset createInstanceByName( String datasetName, String size, String nr, String qlen ) throws IOException {
		Dataset dataset = new DiskBasedDataset(datasetName, size, nr, qlen);
		dataset.createRuleSet();
		dataset.addStat();
		return dataset;
	}
	
	public static WindowDataset createWindowInstanceByName( String datasetName, String size, String nr, String qlen ) throws IOException {
		WindowDataset dataset = new WindowDataset(datasetName, size, nr, qlen);
		PkwiseTokenOrder.run(dataset, Integer.parseInt(qlen));
		dataset.loadRecordList(dataset.searchedPath);
		dataset.buildRecordStore();
		dataset.ruleSet = new Ruleset();
		dataset.addStat();
//		dataset.ruleSet.writeToFile();
		return dataset;
	}

	public static TransWindowDataset createTransWindowInstanceByName( String datasetName, String size, String nr, String qlen, String theta ) throws IOException {
		TransWindowDataset dataset = new TransWindowDataset(datasetName, size, nr, qlen, theta);
		PkwiseTokenOrder.run(dataset, Integer.parseInt(qlen));
		dataset.loadRecordList(dataset.searchedPath);
		dataset.buildRecordStore();
		dataset.createRuleSet();
		dataset.buildIntQGramStore();
		dataset.addStat();
//		dataset.ruleSet.writeToFile();
		return dataset;
	}


	protected static String setName( String name, String size, String nr, String qlen ) {
		StringBuilder strbld = new StringBuilder(name);
		if ( size != null ) strbld.append("_n"+size);
		if ( nr != null ) strbld.append("_r"+nr);
		if ( qlen != null ) strbld.append("_q"+qlen);
		return strbld.toString();
	}
	
	protected Dataset( String datasetName, String size, String nr, String qlen ) {
		name = setName(datasetName, size, nr, qlen);
		searchedPath = DatasetInfo.getSearchedPath(datasetName, qlen);
		indexedPath = DatasetInfo.getIndexedPath(datasetName, size);
		rulePath = DatasetInfo.getRulePath(datasetName, nr);
		outputPath = "output";
		statContainer = new StatContainer();
		statContainer.setStat(Stat.Dataset_Name, name);
		statContainer.setStat(Stat.Dataset_nt, size);
		statContainer.setStat(Stat.Dataset_nr, nr);
		statContainer.setStat(Stat.Dataset_qlen, qlen);
	}
	
	protected void initTokenIndex() {
		statContainer.startWatch("Time_TokenOrder");
		TokenOrder order = new TokenOrder(this);
		Record.tokenIndex = order.getTokenIndex();
		statContainer.stopWatch("Time_TokenOrder");
	}
	
	protected void createRuleSet() {
		ruleSet = new Ruleset(this);
		Rule.automata = new ACAutomataR(ruleSet.get());
	}
	
	protected void addStat() {
		Iterable<Record> searchedList = getSearchedList();
		Iterable<Record> indexedList = getIndexedList();
		statContainer.setStat(Stat.Dataset_numSearched, Integer.toString(getSize(searchedList)));
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(getSize(indexedList)));
		statContainer.setStat(Stat.Dataset_numRule, Integer.toString(ruleSet.size()));
		statContainer.setStat(Stat.Len_SearchedAll, Long.toString(getLengthSum(searchedList)));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(getLengthSum(indexedList)));
		statContainer.finalize();
	}
	
	public abstract Iterable<Record> getSearchedList();

	public abstract Iterable<Record> getIndexedList();
	
	public abstract Record getRecord(int id);

	protected Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : getSearchedList() ) tokenSet.addAll( rec.getTokens() );
		for ( Record rec : getIndexedList() ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}
	
	protected long getLengthSum( Iterable<Record> recordList ) {
		long sum = 0;
		for ( Record rec : recordList ) sum += rec.size();
		return sum;
	}
	
	protected int getSize( Iterable<Record> recordList ) {
		int n = 0;
		for ( @SuppressWarnings("unused") Record rec : recordList ) ++n;
		return n;
	}
}

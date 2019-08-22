package snu.kdd.substring_syn.data;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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
		String name = cmd.getOptionValue( "data" );
		String size = cmd.getOptionValue( "nt" );
		return createInstanceByName(name, size);
	}

	@Deprecated
	public static Dataset createInstanceByPath( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String searchedPath = cmd.getOptionValue( "searchedPath" );
		final String indexedPath = cmd.getOptionValue( "indexedPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		final String name = setName(searchedPath, indexedPath, rulePath);
		return new InMemDataset( name, rulePath, searchedPath, indexedPath, outputPath );
	}

	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		final String rulePath = DatasetInfo.getRulePath(name);
		final String searchedPath = DatasetInfo.getSearchedPath(name, size);
		final String indexedPath = DatasetInfo.getIndexedPath(name, size);
		final String outputPath = "output";
		Dataset dataset = new DiskBasedDataset( name+"_"+size, rulePath, searchedPath, indexedPath, outputPath );
		dataset.createRuleSet();
		dataset.initStat();
		return dataset;
	}

	protected static String setName( String searchedPath, String indexedPath, String rulePath ) {
		String searchedFileName = searchedPath.substring( searchedPath.lastIndexOf(File.separator) + 1 );
		String indexedFileName = indexedPath.substring( indexedPath.lastIndexOf(File.separator) + 1 );
		String ruleFileName = rulePath.substring( rulePath.lastIndexOf(File.separator) + 1 );
		return searchedFileName + "_" + indexedFileName + "_" + ruleFileName;
	}

	protected Dataset( String name, String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		this.name = name;
		this.searchedPath = searchedPath;
		this.indexedPath = indexedPath;
		this.rulePath = rulePath;
		this.outputPath = outputPath;
		statContainer = new StatContainer();
		statContainer.startWatch("Time_TokenOrder");
		TokenOrder order = new TokenOrder(this);
		Record.tokenIndex = order.getTokenIndex();
		statContainer.stopWatch("Time_TokenOrder");
	}
	
	protected void createRuleSet() {
		ruleSet = new Ruleset(this);
		Rule.automata = new ACAutomataR(ruleSet.get());
	}
	
	protected void initStat() {
		Iterable<Record> searchedList = getSearchedList();
		Iterable<Record> indexedList = getIndexedList();
		statContainer.setStat(Stat.Dataset_Name, name);
		statContainer.setStat(Stat.Dataset_numSearched, Integer.toString(getSize(searchedList)));
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(getSize(indexedList)));
		statContainer.setStat(Stat.Dataset_numRule, Integer.toString(ruleSet.size()));
		statContainer.setStat(Stat.Len_SearchedAll, Long.toString(getLengthSum(searchedList)));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(getLengthSum(indexedList)));
		statContainer.finalize();
	}
	
	public abstract Iterable<Record> getSearchedList();

	public abstract Iterable<Record> getIndexedList();

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

package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Dataset {
	
	public final String name;
	public final Ruleset ruleSet;
	public final List<Record> indexedList;
	public final List<Record> searchedList;
	public final String outputPath;
	
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
		return new Dataset( name, rulePath, searchedPath, indexedPath, outputPath );
	}

	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		final String rulePath = DatasetInfo.getRulePath(name);
		final String searchedPath = DatasetInfo.getSearchedPath(name, size);
		final String indexedPath = DatasetInfo.getIndexedPath(name, size);
		final String outputPath = "output";
		return new Dataset( name+"_"+size, rulePath, searchedPath, indexedPath, outputPath );
	}

	private static String setName( String searchedPath, String indexedPath, String rulePath ) {
		String searchedFileName = searchedPath.substring( searchedPath.lastIndexOf(File.separator) + 1 );
		String indexedFileName = indexedPath.substring( indexedPath.lastIndexOf(File.separator) + 1 );
		String ruleFileName = rulePath.substring( rulePath.lastIndexOf(File.separator) + 1 );
		return searchedFileName + "_" + indexedFileName + "_" + ruleFileName;
	}

	private Dataset( String name, String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		this.name = name;
		this.outputPath = outputPath;
		TokenIndex tokenIndex = new TokenIndex();

		indexedList = loadRecordList(indexedPath, tokenIndex);
		searchedList = loadRecordList(searchedPath, tokenIndex);
		ruleSet = new Ruleset( rulePath, getDistinctTokens(), tokenIndex );
		Record.tokenIndex = tokenIndex;
		preprocess();
	}

	private List<Record> loadRecordList( String dataPath, TokenIndex tokenIndex ) throws IOException {
		List<Record> recordList = new ObjectArrayList<>();
		BufferedReader br = new BufferedReader( new FileReader( dataPath ) );
		String line;
		for ( int i=0; ( line = br.readLine() ) != null; ++i ) {
			recordList.add( new Record( i, line, tokenIndex ) );
		}
		br.close();
		return recordList;
	}
	
	private Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : searchedList ) tokenSet.addAll( rec.getTokens() );
		for ( Record rec : indexedList ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}
	
	private void preprocess() {
		for( final Record record : searchedList ) {
			record.preprocessApplicableRules( getAutomataR() );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}
		for( final Record record : indexedList ) {
			record.preprocessApplicableRules( getAutomataR() );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}
		TokenOrder order = new TokenOrder(this);
		reindexByOrder(order);
	}

	public void reindexByOrder( TokenOrder order ) {
		reindexRecords(order);
		reindexRules(order);
		updateTokenIndex(order);
	}
	
	private void reindexRecords( TokenOrder order ) {
		for ( Record rec : indexedList ) rec.reindex(order);
		for ( Record rec : searchedList ) rec.reindex(order);
	}
	
	private void reindexRules( TokenOrder order ) {
		for ( Rule rule : ruleSet.ruleList ) rule.reindex(order);
		ruleSet.automata = new ACAutomataR(ruleSet.get());
	}
	
	private void updateTokenIndex( TokenOrder order ) {
		TokenIndex tokenIndex = order.getTokenIndex();
		Record.tokenIndex = tokenIndex;
		tokenIndex.writeToFile();
	}
	
	public ACAutomataR getAutomataR() {
		return ruleSet.automata;
	}
}

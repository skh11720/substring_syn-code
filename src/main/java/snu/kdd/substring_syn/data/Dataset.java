package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Dataset {
	
	public final DataInfo dataInfo;
	public final Ruleset ruleSet;
	public final List<Record> indexedList;
	public final List<Record> searchedList;
	public final String outputPath;
	public final boolean selfJoin;

	public static Dataset createInstance( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String searchedPath = cmd.getOptionValue( "searchedPath" );
		final String indexedPath = cmd.getOptionValue( "indexedPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		return new Dataset( rulePath, searchedPath, indexedPath, outputPath );
	}
	
	public Dataset( String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		this.dataInfo = new DataInfo(searchedPath, indexedPath, rulePath);
		this.outputPath = outputPath;
		TokenIndex tokenIndex = new TokenIndex();

		if( indexedPath.equals( searchedPath ) ) selfJoin = true;
		else selfJoin = false;

		indexedList = loadRecordList(indexedPath, tokenIndex);
		if( selfJoin ) searchedList = indexedList;
		else searchedList = loadRecordList(searchedPath, tokenIndex);
		ruleSet = new Ruleset( rulePath, getDistinctTokens(), tokenIndex );
		Record.tokenIndex = tokenIndex;
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
		if ( !selfJoin ) 
			for ( Record rec : indexedList ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}

	public void reindexByOrder( TokenOrder order ) {
		reindexRecords(order);
		reindexRules(order);
		updateTokenIndex(order);
	}
	
	private void reindexRecords( TokenOrder order ) {
		for ( Record rec : indexedList ) rec.reindex(order);
		if ( !selfJoin ) {
			for ( Record rec : searchedList ) rec.reindex(order);
		}
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

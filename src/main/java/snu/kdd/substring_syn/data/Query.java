package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class Query {
	
	public final DataInfo dataInfo;
	public final Ruleset ruleSet;
	public final Dataset indexedSet;
	public final Dataset searchedSet;
	public final String outputPath;
	public final boolean selfJoin;

	public static Query parseQuery( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String searchedPath = cmd.getOptionValue( "searchedPath" );
		final String indexedPath = cmd.getOptionValue( "indexedPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		return new Query( rulePath, searchedPath, indexedPath, outputPath );
	}
	
	public Query( String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		this.dataInfo = new DataInfo(searchedPath, indexedPath, rulePath);
		this.outputPath = outputPath;
		TokenIndex tokenIndex = new TokenIndex();

		if ( searchedPath == null ) searchedPath = indexedPath;
		if( indexedPath.equals( searchedPath ) ) selfJoin = true;
		else selfJoin = false;
		indexedSet = new Dataset( indexedPath, tokenIndex );
		if( selfJoin ) searchedSet = indexedSet;
		else searchedSet = new Dataset( searchedPath, tokenIndex );
		ruleSet = new Ruleset( rulePath, getDistinctTokens(), tokenIndex );
		Record.tokenIndex = tokenIndex;
	}
	
	private Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : searchedSet ) tokenSet.addAll( rec.getTokens() );
		if ( !selfJoin ) 
			for ( Record rec : indexedSet ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}

	public void reindexByOrder( TokenOrder order ) {
		reindexRecords(order);
		reindexRules(order);
		updateTokenIndex(order);
	}
	
	private void reindexRecords( TokenOrder order ) {
		for ( Record rec : indexedSet ) rec.reindex(order);
		if ( !selfJoin ) {
			for ( Record rec : searchedSet ) rec.reindex(order);
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
	
	public String getRulePath() {
		return ruleSet.path;
	}
	
	public String getIndexedPath() {
		return indexedSet.path;
	}
	
	public String getSearchedPath() {
		return searchedSet.path;
	}
	
	public ACAutomataR getAutomataR() {
		return ruleSet.automata;
	}
}

package snu.kdd.substring_syn.data;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

public class Query {
	
	public final DataInfo dataInfo;
	public final Ruleset ruleSet;
	public final Dataset indexedSet;
	public final Dataset searchedSet;
	public final String outputPath;
	public final boolean oneSideJoin; // TODO: remove this variable
	public final boolean selfJoin;

	public static Query parseQuery( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String dataOnePath = cmd.getOptionValue( "dataOnePath" );
		final String dataTwoPath = cmd.getOptionValue( "dataTwoPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		Boolean oneSideJoin = Boolean.parseBoolean( cmd.getOptionValue( "oneSideJoin" ) );
		return new Query( rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath );
	}

	public Query( String rulePath, String searchedPath, String indexedPath, boolean oneSideJoin, String outputPath ) throws IOException {
		this.dataInfo = new DataInfo(searchedPath, indexedPath, rulePath);
		this.outputPath = outputPath;
		this.oneSideJoin = oneSideJoin;
		TokenIndex tokenIndex = new TokenIndex();

		if ( searchedPath == null ) searchedPath = indexedPath;
		if( indexedPath.equals( searchedPath ) ) selfJoin = true;
		else selfJoin = false;
		indexedSet = new Dataset( indexedPath, tokenIndex );
		if( selfJoin ) searchedSet = indexedSet;
		else searchedSet = new Dataset( searchedPath, tokenIndex );
		ruleSet = new Ruleset( rulePath, searchedSet, tokenIndex );
		Record.tokenIndex = tokenIndex;
	}

//	public Query( Ruleset ruleSet, Dataset indexedSet, Dataset searchedSet, TokenIndex tokenIndex, boolean oneSideJoin, boolean selfJoin, String outputPath ) {
//		this.dataInfo = null;
//		this.ruleSet = ruleSet;
//		this.indexedSet = indexedSet;
//		this.searchedSet = searchedSet;
//		Record.tokenIndex = tokenIndex;
//		this.oneSideJoin = oneSideJoin;
//		this.outputPath = outputPath;
//		this.selfJoin = selfJoin;
//	}
	
	public void reindexByOrder( TokenOrder order ) {
		reindexRecords(order);
		reindexRules(order);
		updateTokenIndex(order);
	}
	
	private void reindexRecords( TokenOrder order ) {
		for ( Record rec : indexedSet.recordList ) rec.reindex(order);
		if ( !selfJoin ) {
			for ( Record rec : searchedSet.recordList ) rec.reindex(order);
		}
	}
	
	private void reindexRules( TokenOrder order ) {
		for ( Rule rule : ruleSet.ruleList ) rule.reindex(order);
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
}

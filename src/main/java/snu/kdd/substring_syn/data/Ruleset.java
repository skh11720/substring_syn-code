package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Ruleset {
	final String path;
	final ObjectArrayList<Rule> ruleList;

	public Ruleset( String rulePath, Iterable<Integer> distinctTokens, TokenIndex tokenIndex ) throws IOException {
		this.path = rulePath;
		this.ruleList = new ObjectArrayList<>();
		
		createSelfRules(distinctTokens);
		loadRulesFromFile(tokenIndex);
	}
	
	private void createSelfRules( Iterable<Integer> distinctTokens ) {
		for ( int token : distinctTokens )
			ruleList.add( Rule.createRule(token, token) );
	}

	private void loadRulesFromFile( TokenIndex tokenIndex ) throws IOException {
		BufferedReader br = new BufferedReader( new FileReader( path ) );
		String line;
		while( ( line = br.readLine() ) != null ) {
			this.ruleList.add( Rule.createRule( line, tokenIndex ) );
		}
		br.close();
	}

	public Iterable<Rule> get() {
		return this.ruleList;
	}

	public int size() {
		return this.ruleList.size();
	}
}

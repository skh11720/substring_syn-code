package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Ruleset {
	final String path;
	final ObjectArrayList<Rule> ruleList;
	public ACAutomataR automata;

	public Ruleset( String rulePath, Dataset searchedSet, TokenIndex tokenIndex ) throws IOException {
		this.path = rulePath;
		this.ruleList = new ObjectArrayList<>();
		
		createSelfRules(searchedSet);
		loadRulesFromFile(tokenIndex);
		
		automata = new ACAutomataR(ruleList);
	}
	
	private void createSelfRules( Dataset searchedSet ) {
		IntOpenHashSet processedTokenSet = new IntOpenHashSet();
		for ( Record recS : searchedSet ) {
			for ( int token : recS.getTokens() ) {
				if ( !processedTokenSet.contains(token) ) {
					processedTokenSet.add(token);
					ruleList.add( Rule.createRule(token, token) );
				}
			}
		}
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

package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.utils.Log;

public class Ruleset {
	final ObjectArrayList<Rule> ruleList;

	public Ruleset( Dataset dataset ) {
		this.ruleList = new ObjectArrayList<>();
		createSelfRules(dataset.getDistinctTokens());
		loadRulesFromFile(dataset.rulePath);
		Log.log.info("Ruleset created: %d rules", size());
	}
	
	public void createSelfRules( Iterable<Integer> distinctTokens ) {
		for ( int token : distinctTokens )
			ruleList.add( Rule.createRule(token, token) );
	}

	private void loadRulesFromFile( String path ) {
		try {
			BufferedReader br = new BufferedReader( new FileReader( path ) );
			String line;
			while( ( line = br.readLine() ) != null ) {
				this.ruleList.add( Rule.createRule(line) );
			}
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public Iterable<Rule> get() {
		return this.ruleList;
	}

	public int size() {
		return this.ruleList.size();
	}
}

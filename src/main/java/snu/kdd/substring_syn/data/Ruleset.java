package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.io.PrintStream;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.utils.Log;

public class Ruleset {
	final ObjectArrayList<Rule> ruleList;
	
	public Ruleset() {
		this.ruleList = new ObjectArrayList<>();
	}

	public Ruleset( Dataset dataset ) {
		this.ruleList = new ObjectArrayList<>();
		createSelfRules(dataset.getDistinctTokens());
		loadRulesFromDataset(dataset);
		Log.log.info("Ruleset created: %d rules", size());
	}
	
	public void createSelfRules( Iterable<Integer> distinctTokens ) {
		for ( int token : distinctTokens )
			ruleList.add( Rule.createRule(token, token) );
	}

	private void loadRulesFromDataset(Dataset dataset) {
		for ( Rule rule : dataset.getRules() ) ruleList.add(rule);
	}

	public Iterable<Rule> get() {
		return this.ruleList;
	}

	public int size() {
		return this.ruleList.size();
	}
	
	public void writeToFile() {
		try {
			PrintStream ps = new PrintStream("tmp/Ruleset.txt");
			for ( Rule rule : ruleList ) {
				ps.println(rule.toString() +"\t\t" + rule.toOriginalString());
			}
			ps.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

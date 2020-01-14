package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.io.PrintStream;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;
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
			ruleList.add( createSelfRule(token) );
	}

	protected Rule createSelfRule( int token ) {
		int[] lhs = new int[] {token};
		int[] rhs = new int[] {token};
		int id = ruleList.size();
		return new Rule(id, lhs, rhs);
	}
	
	private void loadRulesFromDataset(Dataset dataset) {
		for ( String ruleStr : dataset.getRuleStrs() ) {
			ruleList.add(createRule(ruleStr));
		}
	}

	protected Rule createRule( String str ) {
		String[][] rstr = tokenize(str);
		int[] lhs = getTokenIndexArray(rstr[0]);
		int[] rhs = getTokenIndexArray(rstr[1]);
		int id = ruleList.size();
		return new Rule(id, lhs, rhs);
	}

	public static String[][] tokenize( String str ) {
		String[][] rstr = new String[2][];
		String[] tokens = str.toLowerCase().split("\\|\\|\\|");
		rstr[0] = tokens[0].trim().split(" ");
		rstr[1] = tokens[1].trim().split(" ");
		return rstr;
	}

	protected int[] getTokenIndexArray( String[] tokenArr ) {
		int[] indexArr = new int[tokenArr.length];
		for ( int i=0; i<tokenArr.length; ++i ) {
			indexArr[i] = Record.tokenIndex.getIDOrAdd(tokenArr[i]);
		}
		return indexArr;
	}
	
	public final Rule getRule(int id) {
		return ruleList.get(id);
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

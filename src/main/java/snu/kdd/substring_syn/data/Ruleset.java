package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StringSplitIterator;

public class Ruleset {
	private final ObjectArrayList<Rule> ruleList;
	
	public Ruleset() {
		this.ruleList = new ObjectArrayList<>();
	}

	public Ruleset(Iterable<Integer> distinctTokens, Iterable<String> ruleStrings) {
		this.ruleList = new ObjectArrayList<>();
		createSelfRules(distinctTokens);
		loadRulesFromDataset(ruleStrings);
		Log.log.info("Ruleset created: %d rules", size());
	}
	
	public void createSelfRules( Iterable<Integer> distinctTokens ) {
		for ( int token : distinctTokens )
			ruleList.add( createSelfRule(token) );
	}

	protected Rule createSelfRule( int token ) {
		int[] lhs = new int[] {token};
		int[] rhs = lhs;
		int idx = ruleList.size();
		return new Rule(idx, lhs, rhs);
	}
	
	private void loadRulesFromDataset(Iterable<String> ruleStrings) {
		for ( String ruleStr : ruleStrings ) {
			ruleList.add(createRule(ruleStr));
		}
	}

	protected Rule createRule( String str ) {
		int[] lhs = getTokenIndexArray(getLhs(str));
		int[] rhs = getTokenIndexArray(getRhs(str));
		int idx = ruleList.size();
		return new Rule(idx, lhs, rhs);
	}

//	public static String[][] tokenize( String str ) {
//		String[][] rstr = new String[2][];
//		String[] tokens = str.toLowerCase().split("\\|\\|\\|");
//		rstr[0] = tokens[0].trim().split(" ");
//		rstr[1] = tokens[1].trim().split(" ");
//		return rstr;
//	}
	
	public static String getLhs( String str ) {
		int p = str.indexOf('|');
		return str.substring(0, p).trim().toLowerCase();
	}
	
	public static String getRhs( String str ) {
		int p = str.lastIndexOf('|');
		return str.substring(p+1, str.length()).trim().toLowerCase();
	}

	public static int[] getTokenIndexArray( String str ) {
		int size = (int)str.chars().filter(x->x == ' ').count()+1;
		int[] indexArr = new int[size];
		StringSplitIterator tokenIter = new StringSplitIterator(str);
		for ( int i=0; tokenIter.hasNext(); ++i ) {
			indexArr[i] = Record.tokenIndex.getIDOrAdd(tokenIter.next());
		}
		return indexArr;
	}
	
	public final Rule getRule(int idx) {
		return ruleList.get(idx);
	}

	public Iterable<Rule> get() {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return ruleList.stream().distinct().iterator();
			}
		};
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

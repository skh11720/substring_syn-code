package snu.kdd.substring_syn.data;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class TokenIndexBuilder {
	
	public static TokenIndex build(Dataset dataset) {
		TokenIndexBuilder builder = new TokenIndexBuilder();
		return builder.getTokenIndex(dataset);
	}
	
	protected final Int2IntOpenHashMap counter;
	
	protected TokenIndexBuilder() {
		Log.log.trace("TokenOrder.constructor");
		counter = initCounter();
	}

	protected final Int2IntOpenHashMap initCounter() {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue(0);
		return counter;
	}
	
	public final TokenIndex getTokenIndex(Dataset dataset) {
		Log.log.trace("TokenOrder.getTokenIndex()");
		countTokens(dataset);
		TokenIndex tokenIndex = buildNewTokenIndex();
		return tokenIndex;
	}

	protected void countTokens(Dataset dataset) {
		Record.tokenIndex = new TokenIndex();
		countTokensFromRecords(dataset.getIndexedList());
		countTokensFromRules(dataset.getRuleStrs());
	}

	protected final void countTokensFromRecords(Iterable<Record> records) {
		for ( Record rec : records ) {
			for ( int token : rec.getTokenArray() ) counter.addTo(token, 1);
		}
	}
	
	protected final void countTokensFromRules(Iterable<String> ruleStrs) {
		for ( String ruleStr : ruleStrs ) {
			String[][] rstr = Ruleset.tokenize(ruleStr);
			int[] rhs = Rule.getTokenIndexArray(rstr[1]);
			for ( int token : rhs ) counter.addTo(token, 1);
		}
	}
	
	protected final TokenIndex buildNewTokenIndex() {
		TokenIndex tokenIndex = new TokenIndex();
		Iterator<Int2IntMap.Entry> iter = counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntValue) ).iterator();
		while ( iter.hasNext() ) {
			int tidx = iter.next().getIntKey();
			tokenIndex.add(Record.tokenIndex.getToken(tidx));
		}
		return tokenIndex;
	}
	
	public final void writeToFile() throws FileNotFoundException {
		PrintStream ps = new PrintStream("tmp/TokenOrder.txt");
		Iterator<Int2IntMap.Entry> iter = counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntValue) ).iterator();
		for ( int i=0; iter.hasNext(); ++i ) {
			int tidx = iter.next().getIntKey();
			ps.println(i+"\t"+tidx+"\t"+Record.tokenIndex.getToken(tidx));
		}
		ps.close();
	}
}

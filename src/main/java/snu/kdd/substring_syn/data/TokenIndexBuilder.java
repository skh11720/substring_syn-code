package snu.kdd.substring_syn.data;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;

public class TokenIndexBuilder {
	
	protected final Iterable<TransformableRecordInterface> indexedRecords;
	protected final Iterable<String> ruleStrings;
	protected final Int2IntOpenHashMap counter;

	public static TokenIndex build(Iterable<TransformableRecordInterface> indexedRecords, Iterable<String> ruleStrings) {
		TokenIndexBuilder builder = new TokenIndexBuilder(indexedRecords, ruleStrings);
		return builder.getTokenIndex();
	}
	
	protected TokenIndexBuilder(Iterable<TransformableRecordInterface> indexedRecords, Iterable<String> ruleStrings) {
		Log.log.trace("TokenOrder.constructor");
		this.indexedRecords = indexedRecords;
		this.ruleStrings = ruleStrings;
		counter = initCounter();
	}

	protected final Int2IntOpenHashMap initCounter() {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue(0);
		return counter;
	}
	
	public final TokenIndex getTokenIndex() {
		Log.log.trace("TokenOrder.getTokenIndex()");
		countTokens();
		TokenIndex tokenIndex = buildNewTokenIndex();
		return tokenIndex;
	}

	protected void countTokens() {
		Record.tokenIndex = new TokenIndex();
		countTokensFromRecords();
		countTokensFromRules();
	}

	protected final void countTokensFromRecords() {
		Log.log.trace("TokenOrder.countTokensFromRecords()");
		for ( TransformableRecordInterface rec : indexedRecords ) {
			for ( int token : rec.getTokenArray() ) counter.addTo(token, 1);
		}
	}
	
	protected final void countTokensFromRules() {
		Log.log.trace("TokenOrder.countTokensFromRules()");
		for ( String ruleStr : ruleStrings ) {
			int[] rhs = Ruleset.getTokenIndexArray(Ruleset.getRhs(ruleStr));
			for ( int token : rhs ) counter.addTo(token, 1);
		}
	}
	
	protected final TokenIndex buildNewTokenIndex() {
		Log.log.trace("TokenOrder.buildNewTokenIndex()");
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

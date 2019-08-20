package snu.kdd.substring_syn.data;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Record;

public class TokenOrder {
	
	Int2IntMap orderMap = null;
	Int2IntOpenHashMap counter = null;
	
	public TokenOrder( Dataset dataset ) {
		initCounter();
//		countTokens(dataset.searchedList);
		countTokens(dataset.getIndexedList());
		countTokens(dataset.ruleSet);
		buildOrderMap(counter);
	}
	
	public int getOrder( int id ) {
		return orderMap.get(id);
	}
	
	public TokenIndex getTokenIndex() {
		TokenIndex tokenIndex = new TokenIndex();
		for ( int idx : orderMap.keySet() ) {
			String token = Record.tokenIndex.getToken(idx);
			tokenIndex.add(token);
		}
		return tokenIndex;
	}
	
	public void writeToFile() throws FileNotFoundException {
		PrintStream ps = new PrintStream("tmp/TokenOrder.txt");
		for ( Int2IntMap.Entry entry : orderMap.int2IntEntrySet() ) {
			int id = entry.getIntKey();
			ps.println(id+"\t"+Record.tokenIndex.getToken(id)+"\t"+entry.getIntValue()+"\t"+counter.get(id));
		}
		ps.close();
	}
	
	private void initCounter() {
		counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue(0);
	}
	
	private void countTokens( Iterable<Record> recordList ) {
		for ( Record rec : recordList ) {
			for ( int token : rec.getTokens() ) {
				counter.addTo(token, 1);
			}
		}
	}

	private void countTokens( Ruleset ruleSet ) {
		for ( Rule rule : ruleSet.ruleList ) {
			for ( int token : rule.getRhs() ) {
				counter.addTo(token, 1);
			}
		}
	}
		
	private void buildOrderMap( Int2IntMap counter ) {
		orderMap = new Int2IntLinkedOpenHashMap(counter.size());
		IntStream orderedKeyStream = counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntValue)).mapToInt(e -> e.getIntKey());
		orderedKeyStream.forEach(key -> orderMap.put(key, orderMap.size()));
	}
}

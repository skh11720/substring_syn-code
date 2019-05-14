package snu.kdd.substring_syn.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.utils.Util;

public class TokenOrder implements Comparator<Integer> {
	
	Int2IntMap orderMap = null;
	Int2IntOpenHashMap counter = null;
	
	public TokenOrder( Collection<Record> records ) {
		counter = countTokens(records);
		buildOrderMap(counter);
	}
	
	private Int2IntOpenHashMap countTokens( Collection<Record> records ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue(0);
		for ( Record rec : records ) {
//			System.out.println( Arrays.toString(rec.getTokensArray()) );
			for ( int token : rec.getTokens() ) {
				counter.addTo(token, 1);
//				System.out.println(token+"\t"+counter.get(token));
			}
		}
		return counter;
	}
		
	private void buildOrderMap( Int2IntMap counter ) {
		orderMap = new Int2IntLinkedOpenHashMap(counter.size());
		IntStream orderedKeyStream = counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntValue)).mapToInt(e -> e.getIntKey());
		orderedKeyStream.forEach(key -> orderMap.put(key, orderMap.size()));
	}
	
	@Override
	public int compare(Integer o1, Integer o2) {
		return Integer.compare(orderMap.get(o1), orderMap.get(o2));
	}

	public void writeToFile() throws FileNotFoundException {
		PrintStream ps = new PrintStream("tmp/TokenOrder.txt");
		for ( Int2IntMap.Entry entry : orderMap.int2IntEntrySet() ) {
			int id = entry.getIntKey();
			ps.println(id+"\t"+Record.tokenIndex.getToken(id)+"\t"+entry.getIntValue()+"\t"+counter.get(id));
		}
		ps.close();
	}
	
	
	public static void main(String[] args) throws IOException {
		Query query = Util.getQuery("SPROT", 10000);
		TokenOrder tokenOrder = new TokenOrder(query.searchedSet.recordList);
		tokenOrder.writeToFile();
	}
}

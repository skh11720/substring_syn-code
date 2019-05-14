package snu.kdd.substring_syn.data;

import java.util.Collection;
import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class TokenOrder implements Comparator<Integer> {
	
	Int2IntMap orderMap = new Int2IntOpenHashMap();
	
	public TokenOrder( Collection<Record> records ) {
		Int2IntOpenHashMap counter = countTokens(records);
	}
	
	private Int2IntOpenHashMap countTokens( Collection<Record> records ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue(0);
		for ( Record rec : records ) {
			for ( int token : rec.getTokens() ) counter.addTo(token, 1);
		}
		return counter;
	}
		
	private void buildOrderMap( Int2IntMap counter ) {
		counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntValue)).forEach(e -> e.getIntKey());
	}

	@Override
	public int compare(Integer o1, Integer o2) {
		// TODO Auto-generated method stub
		return 0;
	}

}

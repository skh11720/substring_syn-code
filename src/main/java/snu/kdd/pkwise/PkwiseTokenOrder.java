package snu.kdd.pkwise;

import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseTokenOrder {
	
	public static void run( WindowDataset dataset ) {
		PkwiseTokenOrder order = new PkwiseTokenOrder(dataset);
	}

	public PkwiseTokenOrder( WindowDataset dataset ) {
		Int2IntOpenHashMap counter = countTokensFromWindows(dataset);
		Record.tokenIndex = getNewTokenIndex(counter);
	}

	private Int2IntOpenHashMap countTokensFromWindows( WindowDataset dataset ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		for ( Subrecord window : dataset.getWindowList() ) {
			for ( int token : window.getTokenArray() ) {
				counter.addTo(token, 1);
			}
		}
		return counter;
	}
	
	private TokenIndex getNewTokenIndex( Int2IntOpenHashMap counter ) {
		TokenIndex tokenIndex = new TokenIndex();
		Iterator<Int2IntMap.Entry> iter = counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntValue) ).iterator();
		for ( int i=0; iter.hasNext(); ++i ) {
			int tokenIdx = iter.next().getIntKey();
			String token = Record.tokenIndex.getToken(tokenIdx);
			tokenIndex.add(token);
		}
		return tokenIndex;
	}
}

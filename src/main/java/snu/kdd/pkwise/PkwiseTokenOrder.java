package snu.kdd.pkwise;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseTokenOrder {
	
	private final int w;
	
	@SuppressWarnings("unused")
	public static void run( WindowDataset dataset, int w ) {
		PkwiseTokenOrder order = new PkwiseTokenOrder(dataset, w);
	}

	public PkwiseTokenOrder( WindowDataset dataset, int w ) {
		this.w = w;
		Int2IntOpenHashMap counter = countTokensFromWindows(dataset);
		Record.tokenIndex = getNewTokenIndex(counter);
	}

	private Int2IntOpenHashMap countTokensFromWindows( WindowDataset dataset ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		
		for ( Record query : dataset.getSearchedList() ) {
			for ( int token : query.getTokenArray() ) {
				counter.addTo(token, 1);
			}
		}

		for ( Subrecord window : dataset.getWindowList(w) ) {
			for ( int token : window.getTokenArray() ) {
				counter.addTo(token, 1);
			}
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(dataset.rulePath));
			br.lines().forEach(line -> {
				String[][] rstr = Rule.tokenize(line);
				for ( String token : rstr[1] ) {
					int tokenIdx = Record.tokenIndex.getIDOrAdd(token);
					counter.addTo(tokenIdx, 1);
				}
			});
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return counter;
	}
	
	@SuppressWarnings("unused")
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

package snu.kdd.pkwise;

import java.util.Iterator;

import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.TokenIndexBuilder;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;

public class PkwiseTokenIndexBuilder extends TokenIndexBuilder {

	private final int w;

	public static TokenIndex build(Iterable<Record> indexedRecords, Iterable<String> ruleStrings, int w) {
		TokenIndexBuilder builder = new PkwiseTokenIndexBuilder(indexedRecords, ruleStrings, w);
		return builder.getTokenIndex();
	}
	
	private PkwiseTokenIndexBuilder(Iterable<Record> indexedRecords, Iterable<String> ruleStrings, int w) {
		super(indexedRecords, ruleStrings);
		this.w = w;
	}

	@Override
	protected void countTokens() {
		Record.tokenIndex = new TokenIndex();
		countTokensFromRecordWindows();
		countTokensFromRules();
	}

	protected final void countTokensFromRecordWindows() {
		Iterable<RecordInterface> windows = new Iterable<RecordInterface>() {
			
			@Override
			public Iterator<RecordInterface> iterator() {
				return new WindowDataset.WindowIterator(indexedRecords.iterator(), w);
			}
		};

		for ( RecordInterface window : windows ) {
			for ( int token : window.getTokenArray() ) counter.addTo(token, 1);
		}
	}
}

package snu.kdd.pkwise;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.TokenIndex;
import snu.kdd.substring_syn.data.TokenIndexBuilder;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;

public class PkwiseTokenIndexBuilder extends TokenIndexBuilder {

	public static TokenIndex build(WindowDataset dataset, int w) {
		PkwiseTokenIndexBuilder builder = new PkwiseTokenIndexBuilder(w);
		return builder.getTokenIndex(dataset);
	}
	
	private final int w;
	
	private PkwiseTokenIndexBuilder( int w ) {
		this.w = w;
	}

	@Override
	protected void countTokens( Dataset dataset ) {
		WindowDataset wdataset = (WindowDataset)dataset;
		Record.tokenIndex = new TokenIndex();
		countTokensFromRecordWindows(wdataset.getWindowList(w));
		countTokensFromRules(wdataset.getRules());
	}

	protected final void countTokensFromRecordWindows(Iterable<RecordInterface> windows) {
		for ( RecordInterface window : windows ) {
			for ( int token : window.getTokenArray() ) counter.addTo(token, 1);
		}
	}
}

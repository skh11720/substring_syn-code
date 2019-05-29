package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindow;
import vldb18.NaivePkduckValidator;

public class NaiveSearch extends AbstractSearch {

	final NaivePkduckValidator validator;


	public NaiveSearch(double theta) {
		super(theta);
		validator = new NaivePkduckValidator();
	}

	protected void searchRecordFromQuery( Record qrec, Record rec ) {
		/*
		 * TODO: implement
		 */
	}
	
	protected void searchRecordFromText( Record qrec, Record rec ) {
		NaivePkduckValidator validator = new NaivePkduckValidator();
		for ( int w=1; w<=rec.size(); ++w ) {
			RecordSortedSlidingWindow windowSlider = new RecordSortedSlidingWindow(rec, w, theta);
			for ( int widx=0; windowSlider.hasNext(); ++widx ) {
				Subrecord window = windowSlider.next();
				double sim = validator.sim(qrec, window.toRecord());
				if ( sim >= theta ) {
					rsltFromText.add(new IntPair(qrec.getID(), rec.getID()));
					return;
				}
			}
		}
	}

	@Override
	public String getName() {
		return "NaiveSearch";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}

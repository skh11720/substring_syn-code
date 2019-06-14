package snu.kdd.substring_syn.utils.window;

import java.util.Iterator;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;

public class SortedRecordSlidingWindow extends AbstractRecordSlidingWindow {

	public SortedRecordSlidingWindow(Record rec, int w, double theta) {
		super(rec, w, theta);
	}

	@Override
	public Iterator<Subrecord> iterator() {
		return new SortedRecordSlidingWindowIterator(rec, w, theta);
	}

}

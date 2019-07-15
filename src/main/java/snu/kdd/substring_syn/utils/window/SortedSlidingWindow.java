package snu.kdd.substring_syn.utils.window;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.utils.window.iterator.SortedSlidingWindowIterator;

public class SortedSlidingWindow extends AbstractSlidingWindow {

	public SortedSlidingWindow(int[] seq, int w, double theta) {
		super(seq, w, theta);
	}

	@Override
	public Iterator<IntList> iterator() {
		return new SortedSlidingWindowIterator(seq, w, theta);
	}

}

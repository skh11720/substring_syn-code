package snu.kdd.substring_syn.utils.window;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.utils.window.iterator.SimpleSlidingWindowIterator;

public class SimpleSlidingWindow extends AbstractSlidingWindow {
	
	public SimpleSlidingWindow(int[] seq, int w, double theta) {
		super(seq, w, theta);
	}

	@Override
	public Iterator<IntList> iterator() {
		return new SimpleSlidingWindowIterator(seq, w, theta);
	}
}

package snu.kdd.substring_syn.utils.window;

import it.unimi.dsi.fastutil.ints.IntList;

public abstract class AbstractSlidingWindow implements Iterable<IntList> {

	protected final int[] seq;
	protected final int w;
	protected final double theta;

	public AbstractSlidingWindow( int[] seq, int w, double theta ) {
		this.seq = seq;
		this.w = w;
		this.theta = theta;
	}

}

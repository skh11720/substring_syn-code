package snu.kdd.substring_syn.utils.window;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class SimpleSlidingWindow extends AbstractSlidingWindow {

	public SimpleSlidingWindow( int[] seq, int w, double theta ) {
		super(seq, w, theta);
	}
	
	@Override
	public IntSet getPrefix() {
		return new IntOpenHashSet( getWindow().stream().sorted().limit(lenPrefix).iterator() );
	}
}

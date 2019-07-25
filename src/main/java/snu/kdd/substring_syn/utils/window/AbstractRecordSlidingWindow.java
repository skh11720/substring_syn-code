package snu.kdd.substring_syn.utils.window;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public abstract class AbstractRecordSlidingWindow implements Iterable<Subrecord>{

	protected final Record rec;
	protected final int w;
	protected final double theta;
	
	public AbstractRecordSlidingWindow( Record rec, int w, double theta ) {
		this.rec = rec;
		this.w = w;
		this.theta = theta;
	}
}

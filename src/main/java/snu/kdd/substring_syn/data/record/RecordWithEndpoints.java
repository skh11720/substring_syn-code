package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public class RecordWithEndpoints extends Record {
	
	protected final int sp;
	protected final IntList epList;

	public RecordWithEndpoints( Record rec, int sp, IntList epList ) {
		super(rec.id, rec.tokens);
		this.sp = sp;
		this.epList = epList;
	}
	
	public int getStartPoint() {
		return sp;
	}

	public IntList getEndpoints() {
		return epList;
	}
}

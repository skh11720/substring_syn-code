package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.IntList;

public class Subrecord implements RecordInterface {
	
	private final Record rec;
	private final int sidx;
	private final int eidx;

	public Subrecord( Record rec, int sidx, int eidx ) {
		this.rec = rec;
		this.sidx = sidx; // inclusive
		this.eidx = eidx; // exclusive
	}

	@Override
	public IntList getTokenList() {
		return rec.getTokenList().subList(sidx, eidx);
	}

	@Override
	public int getMaxTransLength() {
		// TODO: return more tight (accurate) value
		return rec.getMaxLength();
	}

	@Override
	public int size() {
		return eidx-sidx;
	}

	@Override
	public Rule[] getSuffixApplicableRules(int i) {
		return getSuffixApplicableRules(sidx+i);
	}
	
	@Override
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for ( int i=sidx; i<eidx; ++i ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			rslt.append(rec.getToken(i));
		}
		return rslt.toString();
	}

	@Override
	public String toOriginalString() {
		StringBuilder rslt = new StringBuilder();
		for( int i=sidx; i<eidx; ++i ) {
			rslt.append(Record.tokenIndex.getToken( rec.getToken(i) ) + " ");
		}
		return rslt.toString();
	}
}

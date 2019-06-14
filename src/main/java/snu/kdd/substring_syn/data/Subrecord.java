package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Subrecord implements RecordInterface {
	
	private final Record rec;
	public final int sidx;
	private final int eidx;
	private final int hash;

	public Subrecord( Record rec, int sidx, int eidx ) {
		this.rec = rec;
		this.sidx = sidx; // inclusive
		this.eidx = eidx; // exclusive
		hash = getHash();
	}
	
	private int getHash() {
		int hash = rec.hashCode();
		hash = 0x1f1f1f1f ^ hash+ sidx;
		hash = 0x1f1f1f1f ^ hash+ eidx;
		return hash;
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public IntList getTokenList() {
		return rec.getTokenList().subList(sidx, eidx);
	}

	@Override
	public int getMaxTransLength() {
		// TODO: return more tight (accurate) value
		return rec.getMaxTransLength();
	}

	@Override
	public int size() {
		return eidx-sidx;
	}

	@Override
	public Rule[] getSuffixApplicableRules(int i) {
		return rec.getSuffixApplicableRules(sidx+i);
	}
	
	@Override
	public String toString() {
		StringBuilder rslt = new StringBuilder();
		for ( int i=0; i<rec.size(); ++i ) {
			if( rslt.length() != 0 ) {
				rslt.append(" ");
			}
			if (i == sidx) rslt.append("[");
			rslt.append(rec.getToken(i));
			if (i+1 == eidx) rslt.append("]");
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
	
	public Record toRecord() {
		Record newrec = new Record(getTokenList().toIntArray());
		Rule[][] applicableRules = new Rule[this.size()][];
		for ( int k=sidx; k<eidx; ++k ) {
			ObjectArrayList<Rule> ruleList = new ObjectArrayList<>();
			for ( Rule rule : rec.getApplicableRules(k) ) {
				if ( rule.lhsSize() <= eidx-k ) ruleList.add(rule);
			}
			applicableRules[k-sidx] = new Rule[ruleList.size()];
			ruleList.toArray( applicableRules[k-sidx] );
		}
		newrec.setApplicableRules(applicableRules);
		newrec.preprocessSuffixApplicableRules();
		return newrec;
	}
	
	public Record getSuperRecord() {
		return rec;
	}
}

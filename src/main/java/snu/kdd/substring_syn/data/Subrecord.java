package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Subrecord implements RecordInterface {
	
	private final Record rec;
	public final int sidx;
	public final int eidx;
	private final int hash;

	public Subrecord( RecordInterface rec, int sidx, int eidx ) {
		this.rec = rec.getSuperRecord();
		this.sidx = sidx + rec.getSidx(); // inclusive
		this.eidx = eidx + rec.getSidx(); // exclusive
		hash = getHash();
	}
	
	private int getHash() {
		int hash = rec.hashCode();
		hash = 0x1f1f1f1f ^ hash+ sidx;
		hash = 0x1f1f1f1f ^ hash+ eidx;
		return hash;
	}
	
	@Override
	public int getID() {
		return rec.getID();
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public int getSidx() {
		return sidx;
	}
	
	@Override
	public int getToken(int i) {
		return rec.getToken(sidx+i);
	}
	
	@Override
	public IntList getTokenList() {
		return rec.getTokenList().subList(sidx, eidx);
	}
	
	public int[] getTokenArray() {
		return getTokenList().toIntArray();
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
		for ( int i=sidx; i<eidx; ++i ) {
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

	public String toSuperString() {
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
	public String toStringDetails() {
		return toRecord().toStringDetails();
	}
	
	public Record toRecord() {
		Record newrec = new Record(getTokenList().toIntArray());
		newrec.id = getID();
		Rule[][] applicableRules = new Rule[this.size()][];
		if ( rec.getApplicableRules() != null ) {
			for ( int k=sidx; k<eidx; ++k ) {
				ObjectArrayList<Rule> ruleList = new ObjectArrayList<>();
				for ( Rule rule : rec.getApplicableRules(k) ) {
					if ( rule.lhsSize() <= eidx-k ) ruleList.add(rule);
				}
				applicableRules[k-sidx] = new Rule[ruleList.size()];
				ruleList.toArray( applicableRules[k-sidx] );
			}
		}
		newrec.setApplicableRules(applicableRules);
		newrec.preprocessSuffixApplicableRules();
		return newrec;
	}
	
	public Record getSuperRecord() {
		return rec;
	}

	@Override
	public int getNumApplicableRules() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterable<Rule> getApplicableRuleIterable() {
		// TODO Auto-generated method stub
		return null;
	}
}

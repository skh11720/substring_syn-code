package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Rule;

public interface RecordInterface {
	int getIdx();
	int getID();
	int size();
	int getToken(int i);

	int getSidx();
	Record getSuperRecord();

	int[] getTokenArray();
	IntList getTokenList();
	IntSet getCandTokenSet();

	int getMaxRhsSize();

	Iterable<Rule> getApplicableRuleIterable();
	Iterable<Rule> getApplicableRules( int i );
	Iterable<Rule> getSuffixApplicableRules( int i );
	
	String toString();
	String toOriginalString();
	String toStringDetails();
}

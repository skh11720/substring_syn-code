package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Rule;

public interface RecordInterface {
	int getID();
	int size();
	int getToken(int i);

	int getSidx();
	Record getSuperRecord();

	IntList getTokenList();
	IntSet getCandTokenSet();

	int getMaxTransLength();
	int getMaxRhsSize();

	Iterable<Rule> getApplicableRuleIterable();
	Iterable<Rule> getSuffixApplicableRules(int i);

	String toString();
	String toOriginalString();
	String toStringDetails();
}

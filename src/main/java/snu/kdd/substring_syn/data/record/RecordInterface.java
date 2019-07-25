package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Rule;

public interface RecordInterface {
	int getID();
	int size();
	int getSidx();
	int getToken(int i);
	Record getSuperRecord();
	IntList getTokenList();
	IntSet getCandTokenSet();
	int getMaxTransLength();
	int getMaxRhsSize();
	int getNumApplicableRules();

	Iterable<Rule> getApplicableRuleIterable();
	Iterable<Rule> getSuffixApplicableRules(int i);

	String toString();
	String toOriginalString();
	String toStringDetails();
}

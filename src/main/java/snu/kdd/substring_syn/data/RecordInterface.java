package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.IntList;

public interface RecordInterface {
	int getID();
	int getSidx();
	int getToken(int i);
	Record getSuperRecord();
	IntList getTokenList();
	int getMaxTransLength();
	int size();
	int getNumApplicableRules();
	Iterable<Rule> getApplicableRuleIterable();
	Rule[] getSuffixApplicableRules(int i);
	String toString();
	String toOriginalString();
	String toStringDetails();
}

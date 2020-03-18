package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

public interface RecordInterface {
	int getIdx();
	int getID();
	int size();
	int getToken(int i);

//	int getSidx();
//	Record getSuperRecord();

	int[] getTokenArray();
	IntList getTokenList();
	IntSet getCandTokenSet();

	int getMaxRhsSize();
	
	String toString();
	String toOriginalString();
	String toStringDetails();
}

package snu.kdd.faerie;

import java.math.BigInteger;

public interface FaerieSynIndexInterface {
	
	FaerieSynIndexEntry getEntry(int id);
	Iterable<FaerieSynIndexEntry> getRecordEntries(int id);
	BigInteger diskSpaceUsage();
}

package snu.kdd.faerie;

import java.math.BigInteger;

public interface FaerieSynIndexInterface {
	
	FaerieSynIndexEntry getEntry(int id);
	BigInteger diskSpaceUsage();
}

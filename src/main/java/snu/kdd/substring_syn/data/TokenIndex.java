package snu.kdd.substring_syn.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TokenIndex {
	private final Object2IntOpenHashMap<String> token2IntMap;
	private final ObjectArrayList<String> int2TokenList;

	public TokenIndex() {
		token2IntMap = new Object2IntOpenHashMap<String>();
		token2IntMap.defaultReturnValue(-12345);
		int2TokenList = new ObjectArrayList<String>();
	}
	
	
	public int getIDOrAdd( Substring token ) {
		if ( !token2IntMap.containsKey(token) ) {
			String str = token.toString();
			add(str);
		}
		return getID(token);
	}

	public void add( String str ) {
		int2TokenList.add(str);
		token2IntMap.put(str, int2TokenList.size()-1);
	}


	public int getID( Substring token ) {
		return token2IntMap.getInt(token);
	}

	public String getToken( int index ) {
		return int2TokenList.get( index );
	}
	
	public int getMaxID() {
		return int2TokenList.size()-1;
	}
	
	public String toString( int[] arr ) {
		StringBuilder bld = new StringBuilder();
		for ( int idx : arr ) bld.append( getToken(idx)+' ' );
		return bld.toString().trim();
	}
	
	public void writeToFile() {
		if ( token2IntMap.size() != int2TokenList.size() ) throw new RuntimeException("Size mismatch");
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/TokenIndex.txt")));
			for ( int i=0; i<int2TokenList.size(); ++i ) {
				String token = int2TokenList.get(i);
				pw.println(i+"\t"+token+"\t"+token2IntMap.getInt(token));
			}
			pw.flush();
			pw.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}
}

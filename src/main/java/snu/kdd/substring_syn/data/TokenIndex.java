package snu.kdd.substring_syn.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TokenIndex {
	private final Object2IntOpenHashMap<Token> token2IntMap;
	private final ObjectArrayList<String> int2TokenList;

	public TokenIndex() {
		token2IntMap = new Object2IntOpenHashMap<Token>();
		token2IntMap.defaultReturnValue(-12345);
		int2TokenList = new ObjectArrayList<String>();
	}
	
	public int getIDOrAdd( String token ) {
		if ( !token2IntMap.containsKey(token) ) {
			add(token);
		}
		return getID(token);
	}
	
	public int getIDOrAdd( Substring token ) {
		String str = token.toString();
		if ( !token2IntMap.containsKey(token) ) {
			add(str);
		}
		return getID(str);
	}

	public void add( String str ) {
		if ( !token2IntMap.containsKey(str) ) {
			Token token = new Token(str);
			int2TokenList.add(token.str);
			token2IntMap.put(token, int2TokenList.size()-1);
		}
	}

	public int getID( String token ) {
		return token2IntMap.getInt( new Token(token) );
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
				Token token = new Token(int2TokenList.get(i));
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

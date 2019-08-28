package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Records;

public class TokenOrder {
	
	final Dataset dataset;
	Object2IntOpenHashMap<String> counter = null;
	
	public TokenOrder( Dataset dataset ) {
		this.dataset = dataset;
		initCounter();
		countTokensFromRecords();
		countTokensFromRules();
	}
	
	public TokenIndex getTokenIndex() {
		TokenIndex tokenIndex = new TokenIndex();
		Iterator<Entry<String>> iter = counter.object2IntEntrySet().stream().sorted( Comparator.comparing(Object2IntMap.Entry<String>::getIntValue) ).iterator();
		while ( iter.hasNext() ) {
			String token = iter.next().getKey();
			tokenIndex.add(token);
		}
		return tokenIndex;
	}
	
	public void writeToFile() throws FileNotFoundException {
		PrintStream ps = new PrintStream("tmp/TokenOrder.txt");
		Iterator<Entry<String>> iter = counter.object2IntEntrySet().stream().sorted( Comparator.comparing(Object2IntMap.Entry<String>::getIntValue) ).iterator();
		for ( int i=0; iter.hasNext(); ++i ) {
			String token = iter.next().getKey();
			ps.println(i+"\t"+token+"\t"+counter.getInt(token));
		}
		ps.close();
	}
	
	private void initCounter() {
		counter = new Object2IntOpenHashMap<>();
		counter.defaultReturnValue(0);
	}
	
	private void countTokensFromRecords() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataset.indexedPath));
			br.lines().forEach(line -> {
				String[] pstr = Records.tokenize(line);
				for ( String token : pstr ) counter.addTo(token, 1);
			});
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void countTokensFromRules() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataset.rulePath));
			br.lines().forEach(line -> {
				String[][] rstr = Rule.tokenize(line);
				for ( String token : rstr[1] ) counter.addTo(token, 1);
			});
			br.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

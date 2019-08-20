package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordPreprocess;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class Dataset {
	
	public final String name;
	public final Ruleset ruleSet;
	private final List<Record> indexedList;
	private final List<Record> searchedList;
	private final BufferedReader brIndexed;
	private final BufferedReader brSearched;
	public final String outputPath;
	public final StatContainer statContainer;
	
	public static Dataset createInstance( CommandLine cmd ) throws IOException {
		String name = cmd.getOptionValue( "data" );
		String size = cmd.getOptionValue( "nt" );
		return createInstanceByName(name, size);
	}

	@Deprecated
	public static Dataset createInstanceByPath( CommandLine cmd ) throws IOException {
		final String rulePath = cmd.getOptionValue( "rulePath" );
		final String searchedPath = cmd.getOptionValue( "searchedPath" );
		final String indexedPath = cmd.getOptionValue( "indexedPath" );
		final String outputPath = cmd.getOptionValue( "outputPath" );
		final String name = setName(searchedPath, indexedPath, rulePath);
		return new Dataset( name, rulePath, searchedPath, indexedPath, outputPath );
	}

	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		final String rulePath = DatasetInfo.getRulePath(name);
		final String searchedPath = DatasetInfo.getSearchedPath(name, size);
		final String indexedPath = DatasetInfo.getIndexedPath(name, size);
		final String outputPath = "output";
		return new Dataset( name+"_"+size, rulePath, searchedPath, indexedPath, outputPath );
	}

	private static String setName( String searchedPath, String indexedPath, String rulePath ) {
		String searchedFileName = searchedPath.substring( searchedPath.lastIndexOf(File.separator) + 1 );
		String indexedFileName = indexedPath.substring( indexedPath.lastIndexOf(File.separator) + 1 );
		String ruleFileName = rulePath.substring( rulePath.lastIndexOf(File.separator) + 1 );
		return searchedFileName + "_" + indexedFileName + "_" + ruleFileName;
	}

	private Dataset( String name, String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		this.name = name;
		this.outputPath = outputPath;
		statContainer = new StatContainer();
		TokenIndex tokenIndex = new TokenIndex();

		brIndexed = new BufferedReader( new FileReader(indexedPath) );
		brSearched = new BufferedReader( new FileReader(searchedPath) );
		indexedList = loadRecordList(indexedPath, tokenIndex);
		searchedList = loadRecordList(searchedPath, tokenIndex);
		Log.log.info("[MEM] after loading dataset: %.3f MB", Util.getMemoryUsage());
		ruleSet = new Ruleset( rulePath, getDistinctTokens(), tokenIndex );
		Log.log.info("Ruleset created: %d rules", ruleSet.size());
		Log.log.info("[MEM] after creating rule set: %.3f MB", Util.getMemoryUsage());
		Record.tokenIndex = tokenIndex;
		preprocess();

		statContainer.setStat(Stat.Dataset_Name, name);
		statContainer.setStat(Stat.Dataset_numSearched, Integer.toString(searchedList.size()));
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(indexedList.size()));
		statContainer.setStat(Stat.Dataset_numRule, Integer.toString(ruleSet.size()));
		statContainer.setStat(Stat.Len_SearchedAll, Long.toString(getLengthSum(searchedList)));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(getLengthSum(indexedList)));
		statContainer.finalize();
	}
	
	public Iterable<Record> getSearchedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return searchedList.iterator();
			}
		};
	}

	public Iterable<Record> getIndexedList() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return indexedList.iterator();
			}
		};
	}

	private List<Record> loadRecordList( String dataPath, TokenIndex tokenIndex ) throws IOException {
		List<Record> recordList = new ObjectArrayList<>();
		BufferedReader br = new BufferedReader( new FileReader( dataPath ) );
		String line;
		for ( int i=0; ( line = br.readLine() ) != null; ++i ) {
			recordList.add( new Record( i, line, tokenIndex ) );
		}
		br.close();
		Log.log.info("loadRecordList(%s): %d records", dataPath, recordList.size());
		return recordList;
	}
	
	private Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : searchedList ) tokenSet.addAll( rec.getTokens() );
		for ( Record rec : indexedList ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}
	
	private long getLengthSum( List<Record> recordList ) {
		long sum = 0;
		for ( Record rec : recordList ) sum += rec.size();
		return sum;
	}
	
	private void preprocess() {
		statContainer.startWatch(Stat.Time_Preprocess);
		preprocessByTask(searchedList);
		preprocessByTask(indexedList);
		TokenOrder order = new TokenOrder(this);
		reindexByOrder(order);
		statContainer.stopWatch(Stat.Time_Preprocess);
	}
	
	private void preprocessByRecord() {
		ACAutomataR automata = new ACAutomataR(ruleSet.get());
		for( final Record record : searchedList ) {
			RecordPreprocess.preprocessApplicableRules(record, automata);
			RecordPreprocess.preprocessSuffixApplicableRules(record);
			RecordPreprocess.preprocessTransformLength(record);
//			record.preprocessEstimatedRecords();
		}
		for( final Record record : indexedList ) {
			RecordPreprocess.preprocessApplicableRules(record, automata);
			RecordPreprocess.preprocessSuffixApplicableRules(record);
			RecordPreprocess.preprocessTransformLength(record);
//			record.preprocessEstimatedRecords();
		}
	}
	
	private void preprocessByTask( List<Record> recordList ) {
		ACAutomataR automata = new ACAutomataR(ruleSet.get());
		for ( final Record record : recordList ) RecordPreprocess.preprocessApplicableRules(record, automata);
		Log.log.info("preprocessByTask: preprocessApplicableRules, %d records", recordList.size() );
		Log.log.info("[MEM] after RecordPreprocess.preprocessingApplicableRules: %.3f MB", Util.getMemoryUsage());
		for ( final Record record : recordList ) RecordPreprocess.preprocessSuffixApplicableRules(record);
		Log.log.info("preprocessByTask: preprocessSuffixApplicableRules, %d records", recordList.size() );
		Log.log.info("[MEM] after RecordPreprocess.preprocessSuffixApplicableRules: %.3f MB", Util.getMemoryUsage());
		for ( final Record record : recordList ) RecordPreprocess.preprocessTransformLength(record);
		Log.log.info("preprocessByTask: preprocessTransformLength, %d records", recordList.size() );
		Log.log.info("[MEM] after RecordPreprocess.preprocessTransformLength: %.3f MB", Util.getMemoryUsage());
//		for ( final Record record : recordList ) record.preprocessEstimatedRecords();
//		Log.log.info("preprocessByTask: preprocessEstimatedRecords, %d records", recordList.size() );
	}

	public void reindexByOrder( TokenOrder order ) {
		reindexRecords(order);
		reindexRules(order);
		updateTokenIndex(order);
	}
	
	private void reindexRecords( TokenOrder order ) {
		for ( Record rec : indexedList ) order.reindex(rec);
		for ( Record rec : searchedList ) order.reindex(rec);
	}
	
	private void reindexRules( TokenOrder order ) {
		for ( Rule rule : ruleSet.ruleList ) rule.reindex(order);
	}
	
	private void updateTokenIndex( TokenOrder order ) {
		TokenIndex tokenIndex = order.getTokenIndex();
		Record.tokenIndex = tokenIndex;
		tokenIndex.writeToFile();
	}
}

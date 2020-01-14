package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.pkwise.PkwiseTokenIndexBuilder;
import snu.kdd.pkwise.TransWindowDataset;
import snu.kdd.pkwise.WindowDataset;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.AlgorithmName;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.InputArgument;

public class DatasetFactory {
	
	private static DatasetParam param;

	public static Dataset createInstance( InputArgument arg ) throws IOException {
		param = new DatasetParam(arg);
		AlgorithmName algName = AlgorithmName.valueOf( arg.getOptionValue("alg") );
		if ( algName == AlgorithmName.PkwiseSearch || algName == AlgorithmName.PkwiseNaiveSearch )
			return createWindowInstanceByName(param);
		if ( algName == AlgorithmName.PkwiseSynSearch ) {
			String paramStr = arg.getOptionValue("param");
			String theta = paramStr.split(",")[0].split(":")[1];
			return createTransWindowInstanceByName(param, theta);
		}
		else
			return createInstanceByName(param);
	}
	
	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		return createInstanceByName(new DatasetParam(name, size, null, null, null));
	}

//		Log.log.trace("WindowDataset.initTokenIndex()");
//		statContainer.startWatch("Time_PkwiseTokenIndexBuilder");
//		Record.tokenIndex = PkwiseTokenIndexBuilder.build(this, qlen);
//		statContainer.stopWatch("Time_PkwiseTokenIndexBuilder");
//		Log.log.trace("WindowDataset.initTokenIndex() finished");

	public static Dataset createInstanceByName(DatasetParam param) throws IOException {
		DatasetFactory.param = param;
		Record.tokenIndex = buildTokenIndex();
		Ruleset ruleset = createRuleset();
		RecordStore store = createRecordStore(ruleset);
		DiskBasedDataset dataset = new DiskBasedDataset(param, ruleset, store);
		dataset.addStat();
		dataset.statContainer.finalize();
		return dataset;
	}
	
	public static WindowDataset createWindowInstanceByName(DatasetParam param) throws IOException {
		DatasetFactory.param = param;
		Record.tokenIndex = buildPkwiseTokenIndex();
		Ruleset ruleset = createRuleset();
		RecordStore store = createRecordStore(ruleset);
		WindowDataset dataset = new WindowDataset(param, ruleset, store);
		dataset.addStat();
		dataset.statContainer.finalize();
		return dataset;
	}

	public static TransWindowDataset createTransWindowInstanceByName(DatasetParam param, String theta) throws IOException {
		DatasetFactory.param = param;
		Record.tokenIndex = buildPkwiseTokenIndex();
		Ruleset ruleset = createRuleset();
		RecordStore store = createRecordStore(ruleset);
		TransWindowDataset dataset = new TransWindowDataset(param, ruleset, store, theta);
		dataset.addStat();
		dataset.statContainer.finalize();
		return dataset;
	}

	private static TokenIndex buildTokenIndex() {
		return TokenIndexBuilder.build(indexedRecords(), ruleStrings());
	}

	private static TokenIndex buildPkwiseTokenIndex() {
		return PkwiseTokenIndexBuilder.build(indexedRecords(), ruleStrings(), Integer.parseInt(param.qlen));
	}

	private static Ruleset createRuleset() {
		Ruleset ruleSet = new Ruleset(getDistinctTokens(), ruleStrings());
		Rule.automata = new ACAutomataR(ruleSet.get());
		return ruleSet;
	}
	
	private static RecordStore createRecordStore(Ruleset ruleset) {
		return new RecordStore(indexedRecords(), ruleset);
	}

	protected static final Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : searchedRecords() ) tokenSet.addAll( rec.getTokens() );
		for ( Record rec : indexedRecords() ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}

	private abstract static class AbstractFileBasedIterator<T> implements Iterator<T> {
		
		BufferedReader br;
		Iterator<String> iter;
		int i = 0;
		
		public AbstractFileBasedIterator(String path) {
			try {
				br = new BufferedReader(new FileReader(path));
				iter = br.lines().iterator();
			}
			catch ( IOException e ) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}
	}
	
	static final Iterable<Record> searchedRecords() {
		String searchedPath = DatasetInfo.getSearchedPath(param.name, param.qlen);
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new AbstractFileBasedIterator<Record>(searchedPath) {

					@Override
					public Record next() {
						String line = iter.next();
						return new Record(i++, line);
					}
				};
			}
		};
	}
	
	private static final Iterable<Record> indexedRecords() {
		String indexedPath = DatasetInfo.getIndexedPath(param.name);
		int size = Integer.parseInt(param.size);
		double lenRatio = Double.parseDouble(param.lenRatio);
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new AbstractFileBasedIterator<Record>(indexedPath) {

			        @Override
			        public boolean hasNext() {
						return i < size && iter.hasNext();
			        }

					@Override
					public Record next() {
						String line = getPrefixWithLengthRatio(iter.next());
						return new Record(i++, line);
					}

					private final String getPrefixWithLengthRatio(String str) {
						int nTokens = (int) str.chars().filter(ch -> ch == ' ').count() + 1;
						int eidx=0;
						int len0 = (int)(nTokens*lenRatio);
						int len = 0;
						for ( ; eidx<str.length(); ++eidx ) {
							if ( str.charAt(eidx) == ' ' ) {
								len += 1;
								if ( len == len0 ) break;
							}
						}
						return str.substring(0, eidx);
					}
				};
			}
		};
	}
	
	private static final Iterable<String> ruleStrings() {
		String rulePath = DatasetInfo.getRulePath(param.name, param.nr);
		return new Iterable<String>() {
			
			@Override
			public Iterator<String> iterator() {
				return new AbstractFileBasedIterator<String>(rulePath) {

					@Override
					public String next() {
						return iter.next();
					}
				};
			}
		};
	}
}

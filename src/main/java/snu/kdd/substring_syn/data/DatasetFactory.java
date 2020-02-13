package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.pkwise.PkwiseTokenIndexBuilder;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.AlgorithmName;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public class DatasetFactory {
	
	private static DatasetParam param;
	private static StatContainer statContainer;
	private static boolean isDocInput;

	public static Dataset createInstance( InputArgument arg ) throws IOException {
		setParam(new DatasetParam(arg));
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
	
	private static void setParam(DatasetParam param) {
		DatasetFactory.param = param;
		isDocInput = param.name.endsWith("-DOC");
	}
	
	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		return createInstanceByName(new DatasetParam(name, size, null, null, null));
	}

	public static Dataset createInstanceByName(DatasetParam param) throws IOException {
		statContainer = new StatContainer();
		statContainer.startWatch(Stat.Time_Prepare_Data);
		setParam(param);
		Record.tokenIndex = buildTokenIndex();
		Ruleset ruleset = createRuleset();
		RecordStore store = createRecordStore(ruleset);
		DiskBasedDataset dataset = new DiskBasedDataset(statContainer, param, ruleset, store);
		if (isDocInput) dataset.rid2idpairMap = getRid2IdpairMap();
		statContainer.stopWatch(Stat.Time_Prepare_Data);
		return dataset;
	}
	
	public static WindowDataset createWindowInstanceByName(DatasetParam param) throws IOException {
		statContainer = new StatContainer();
		statContainer.startWatch(Stat.Time_Prepare_Data);
		setParam(param);
		Record.tokenIndex = buildPkwiseTokenIndex();
		Ruleset ruleset = createRuleset();
		RecordStore store = createRecordStore(ruleset);
		WindowDataset dataset = new WindowDataset(statContainer, param, ruleset, store);
		if (isDocInput) dataset.rid2idpairMap = getRid2IdpairMap();
		statContainer.stopWatch(Stat.Time_Prepare_Data);
		return dataset;
	}

	public static TransWindowDataset createTransWindowInstanceByName(DatasetParam param, String theta) throws IOException {
		statContainer = new StatContainer();
		statContainer.startWatch(Stat.Time_Prepare_Data);
		setParam(param);
		Record.tokenIndex = buildPkwiseTokenIndex();
		Ruleset ruleset = createRuleset();
		RecordStore store = createRecordStore(ruleset);
		TransWindowDataset dataset = new TransWindowDataset(statContainer, param, ruleset, store, theta);
		if (isDocInput) dataset.rid2idpairMap = getRid2IdpairMap();
		statContainer.stopWatch(Stat.Time_Prepare_Data);
		return dataset;
	}

	private static TokenIndex buildTokenIndex() {
		Log.log.trace("DataFactory.buildTokenIndex()");
		statContainer.startWatch("Time_DataFactory.buildTokenIndex");
		TokenIndex tokenIndex = TokenIndexBuilder.build(indexedRecords(), ruleStrings());
		statContainer.stopWatch("Time_DataFactory.buildTokenIndex");
		return tokenIndex;
	}

	private static TokenIndex buildPkwiseTokenIndex() {
		Log.log.trace("DataFactory.buildPkwiseTokenIndex()");
		statContainer.startWatch("Time_DataFactory.buildPkwiseTokenIndex");
		TokenIndex tokenIndex = PkwiseTokenIndexBuilder.build(indexedRecords(), ruleStrings(), Integer.parseInt(param.qlen));
		statContainer.stopWatch("Time_DataFactory.buildPkwiseTokenIndex");
		return tokenIndex;
	}

	private static Ruleset createRuleset() {
		Log.log.trace("DataFactory.createRuleset()");
		statContainer.startWatch("Time_DataFactory.createRuleset");
		Ruleset ruleSet = new Ruleset(getDistinctTokens(), ruleStrings());
		Rule.automata = new ACAutomataR(ruleSet.get());
		statContainer.stopWatch("Time_DataFactory.createRuleset");
		return ruleSet;
	}
	
	private static RecordStore createRecordStore(Ruleset ruleset) {
		Log.log.trace("DataFactory.createRecordStore()");
		statContainer.startWatch("Time_DataFactory.createRecordStore");
		RecordStore store = new RecordStore(indexedRecords(), ruleset);
		statContainer.stopWatch("Time_DataFactory.createRecordStore");
		return store;
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

	protected static class DocRecordIterator extends AbstractFileBasedIterator<Record> {
		
		Iterator<String> docIter = null;
		String thisSnt = findNext();
		int nd = 0;
		int did;
		int sid = -1;
		final int size;

		public DocRecordIterator(String path) {
			super(path);
			size = Integer.parseInt(param.size);
		}

		@Override
		public boolean hasNext() {
			return nd < size && thisSnt != null;
		}

		@Override
		public Record next() {
			String snt = thisSnt;
			thisSnt = findNext();
			return new Record(i++, snt);
		}
		
		private final String findNext() {
			while ( iter.hasNext() && (docIter == null || !docIter.hasNext()) ) {
				nd += 1;
				docIter = parseDocument(iter.next());
			}
			if ( docIter != null && docIter.hasNext() ) {
				sid += 1;
				return docIter.next();
			}
			else return null;
		}

		private final Iterator<String> parseDocument(String line) {
			String[] strs = line.split("\t", 2);
			did = Integer.parseInt(strs[0]);
			sid = -1;
			return ObjectArrayList.wrap(strs[1].split("\\|")).iterator();
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
		if (!isDocInput)
			return indexedRecordsInSnt();
		else 
			return indexedRecordsInDocs();
	}
	
	private static final Iterable<Record> indexedRecordsInSnt() {
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
						int len0 = (int)Math.max(1, nTokens*lenRatio);
						int len = 0;
						for ( ; eidx<str.length(); ++eidx ) {
							if ( str.charAt(eidx) == ' ' ) {
								len += 1;
								if ( len >= len0 ) break;
							}
						}
						return str.substring(0, eidx);
					}
				};
			}
		};
	}

	private static final Iterable<Record> indexedRecordsInDocs() {
		String indexedPath = DatasetInfo.getIndexedPath(param.name);
		return new Iterable<Record>() {

			@Override
			public Iterator<Record> iterator() {
				return new DocRecordIterator(indexedPath);
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
	
	private static final Int2ObjectMap<IntPair> getRid2IdpairMap() {
		Int2ObjectMap<IntPair> map = new Int2ObjectOpenHashMap<IntPair>();
		String indexedPath = DatasetInfo.getIndexedPath(param.name);
		DocRecordIterator iter = new DocRecordIterator(indexedPath);
		while (iter.hasNext()) {
			Record rec = iter.next();
			map.put(rec.getID(), new IntPair(iter.did, iter.sid));
		}
		return map;
	}
}

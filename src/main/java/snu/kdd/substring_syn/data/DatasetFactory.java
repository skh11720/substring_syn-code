package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.pkwise.PkwiseTokenIndexBuilder;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.AlgorithmName;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.ReusableRecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.InputArgument;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.StringSplitIterator;

public class DatasetFactory {
	
	private static DatasetParam param;
	private static StatContainer statContainer;
	private static boolean isDocInput;
	private static ACAutomataS ac;

	public static Dataset createInstance( InputArgument arg ) throws IOException {
		setRecordPoolSize(arg);
		param = new DatasetParam(arg);
		AlgorithmName algName = AlgorithmName.valueOf( arg.getOptionValue("alg") );
		if ( algName == AlgorithmName.PkwiseSynSearch ) {
			String paramStr = arg.getOptionValue("param");
			String theta = paramStr.split(",")[0].split(":")[1];
			return createTransWindowInstanceByName(param, theta);
		}
		else
			return createInstanceByName(param);
	}
	
	private static void setRecordPoolSize(InputArgument arg) {
		try {
			RecordPool.BUFFER_SIZE = Integer.parseInt(arg.getOptionValue("pool"));
		}
		catch (Exception e) {}
	}
	
	private static void initCreationProcess(DatasetParam param) {
		Log.log.trace("DatasetFactory.initCreationProcess");
		DatasetFactory.param = param;
		isDocInput = param.name.endsWith("-DOC");
		ac = new ACAutomataS(ruleStrings());
	}
	
	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		return createInstanceByName(new DatasetParam(name, size, null, null, null));
	}

	public static Dataset createInstanceByName(DatasetParam param) throws IOException {
		statContainer = new StatContainer();
		statContainer.startWatch(Stat.Time_Prepare_Data);
		initCreationProcess(param);
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
		initCreationProcess(param);
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
		initCreationProcess(param);
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
		Log.log.trace("DataFactory.getDistinctTokens()");
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : searchedRecords() ) tokenSet.addAll( rec.getTokens() );
		for ( TransformableRecordInterface rec : indexedRecords() ) tokenSet.addAll( rec.getTokenList() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}

	static final Iterable<Record> searchedRecords() {
		String searchedPath = DatasetInfo.getSearchedPath(param.name, param.qlen);
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new AbstractFileBasedIterator<Record>(searchedPath) {

					@Override
					public Record next() {
						Substring line = new Substring(iter.next());
						id += 1;
						Record rec = new Record(idx++, id, line);
						return rec;
					}
				};
			}
		};
	}
	
	
	private static final Iterable<TransformableRecordInterface> indexedRecords() {
		if (!isDocInput)
			return indexedRecordsInSnt();
		else 
			return indexedRecordsInDocs();
	}


	private static final Iterable<TransformableRecordInterface> indexedRecordsInDocs() {
		return new Iterable<TransformableRecordInterface>() {

			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				String indexedPath = DatasetInfo.getIndexedPath(param.name);
				return new DocRecordRawIterator(indexedPath);
			}
		};
	}

	private static final Iterable<TransformableRecordInterface> indexedRecordsInSnt() {
		String indexedPath = DatasetInfo.getIndexedPath(param.name);
		return new Iterable<TransformableRecordInterface>() {
			
			@Override
			public Iterator<TransformableRecordInterface> iterator() {
				return new SntRecordRawIterator(indexedPath);
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
		DocRecordRawIterator iter = new DocRecordRawIterator(indexedPath);
		while (iter.hasNext()) {
			TransformableRecordInterface rec = iter.next();
			map.put(rec.getIdx(), new IntPair(iter.thisDid, iter.thisSid));
		}
		return map;
	}

	private abstract static class AbstractFileBasedIterator<T> implements Iterator<T> {
		
		BufferedReader br;
		Iterator<String> iter;
		int id = 0;
		int idx = 0;
		
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
	
	protected static class SntRecordRawIterator extends AbstractFileBasedIterator<TransformableRecordInterface> {

		final double lenRatio;
		final int size;
		final int narMax;
		Substring nextLine;
		ReusableRecord rec = new ReusableRecord();
		char[] chbuf;
		
		public SntRecordRawIterator(String path) {
			super(path);
			narMax = Integer.parseInt(param.nar);
			size = Integer.parseInt(param.size);
			lenRatio = Double.parseDouble(param.lenRatio);
			nextLine = findNext();
		}

		@Override
		public boolean hasNext() {
			return idx < size && nextLine != null;
		}

		@Override
		public TransformableRecordInterface next() {
			rec.set(idx++, id, nextLine);
			nextLine = findNext();
			return rec;
		}

		protected Substring findNext() {
			Substring line = null;
			while ( iter.hasNext() ) {
				id += 1;
				chbuf = iter.next().toCharArray();
				line = getPrefixWithLengthRatio();
				Iterator<Substring> tokenIter = new StringSplitIterator(line);
				int nar = ac.getNumApplicableRules(tokenIter);
				if ( narMax < 0 || nar <= narMax ) return line;
			}
			return null;
		}

		protected final Substring getPrefixWithLengthRatio() {
			int nTokens = (int) CharBuffer.wrap(chbuf).chars().filter(ch -> ch == ' ').count() + 1;
			int eidx=0;
			int len0 = (int)Math.max(1, nTokens*lenRatio);
			int len = 0;
			for ( ; eidx<chbuf.length; ++eidx ) {
				if ( chbuf[eidx] == ' ' ) {
					len += 1;
					if ( len >= len0 ) break;
				}
			}
			return new Substring(chbuf, 0, eidx);
		}
	}
	

	protected static class DocRecordRawIterator extends AbstractFileBasedIterator<TransformableRecordInterface> {
		
		Iterator<Substring> inDocIter = null;
		String thisSnt;
		int nd = 0;
		int did;
		int sid = -1;
		boolean isDocCounted;
		final int size;
		final int narMax;
		
		int thisDid;
		int thisSid;
		Substring nextLine;
		ReusableRecord rec = new ReusableRecord();

		public DocRecordRawIterator(String path) {
			super(path);
			size = Integer.parseInt(param.size);
			narMax = Integer.parseInt(param.nar);
			nextLine = findNext();
		}

		@Override
		public boolean hasNext() {
			return nd <= size && nextLine != null;
		}

		@Override
		public TransformableRecordInterface next() {
			rec.set(idx++, sid, nextLine);
			thisDid = did;
			thisSid = sid;
			nextLine = findNext();
			return rec;
		}
		
		protected Substring findNext() {
			while (true) {
				while (inDocIter != null && inDocIter.hasNext()) {
					Substring snt = inDocIter.next();
					sid += 1;
					Iterator<Substring> tokenIter = new StringSplitIterator(snt);
					int nar = ac.getNumApplicableRules(tokenIter);
					if ( narMax < 0 || nar <= narMax ) {
						if ( !isDocCounted ) {
							isDocCounted = true;
							nd += 1;
						}
						return snt;
					}
				}
				if ( !iter.hasNext() ) break;
				inDocIter = parseDocument(iter.next());
				isDocCounted = false;
			}
			return null;
		}

		protected final Iterator<Substring> parseDocument(String line) {
			int idxSep = line.indexOf('\t');
			did = Integer.parseInt(line.substring(0, idxSep).toString());
			sid = -1;
			return new StringSplitIterator(new Substring(line), idxSep+1, '|');
		}
	}

}

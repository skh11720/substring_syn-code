package snu.kdd.substring_syn.data;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.pkwise.PkwiseTokenOrder;
import snu.kdd.pkwise.TransWindowDataset;
import snu.kdd.pkwise.WindowDataset;
import snu.kdd.substring_syn.algorithm.search.AlgorithmFactory.AlgorithmName;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class Dataset {
	
	public final String name;
	public final String searchedPath;
	public final String indexedPath;
	public final String rulePath;
	public final String outputPath;
	public final StatContainer statContainer;
	public final int size;
	public final double lenRatio;

	public Ruleset ruleSet;

	public static Dataset createInstance( CommandLine cmd ) throws IOException {
		String name = getOptionValue(cmd, "data");
		String size = getOptionValue(cmd, "nt");
		String nr = getOptionValue(cmd, "nr");
		String qlen = getOptionValue(cmd, "ql");
		String lenRatio = getOptionValue(cmd, "lr");
		DatasetParam param = new DatasetParam(name, size, nr, qlen, lenRatio);
		AlgorithmName algName = AlgorithmName.valueOf( cmd.getOptionValue("alg") );
		if ( algName == AlgorithmName.PkwiseSearch || algName == AlgorithmName.PkwiseNaiveSearch )
			return createWindowInstanceByName(param);
		if ( algName == AlgorithmName.PkwiseSynSearch ) {
			String paramStr = getOptionValue(cmd, "param");
			String theta = paramStr.split(",")[0].split(":")[1];
			return createTransWindowInstanceByName(param, theta);
		}
		else
			return createInstanceByName(param);
	}
	
	static String getOptionValue( CommandLine cmd, String key ) {
		String value = cmd.getOptionValue(key);
		if ( value == null ) throw new RuntimeException("Invalid input argument: "+key+" = "+value);
		return value;
	}

	public static Dataset createInstanceByName( String name, String size ) throws IOException {
		return createInstanceByName(new DatasetParam(name, size, null, null, null));
	}

	public static Dataset createInstanceByName(DatasetParam param) throws IOException {
		DiskBasedDataset dataset = new DiskBasedDataset(param);
		dataset.createRuleSet();
		dataset.addStat();
		dataset.statContainer.finalize();
		return dataset;
	}
	
	public static WindowDataset createWindowInstanceByName(DatasetParam param) throws IOException {
		WindowDataset dataset = new WindowDataset(param);
		PkwiseTokenOrder.run(dataset, Integer.parseInt(param.qlen));
		dataset.loadRecordList(dataset.searchedPath);
		dataset.buildRecordStore();
		dataset.ruleSet = new Ruleset();
		dataset.addStat();
		dataset.statContainer.finalize();
//		dataset.ruleSet.writeToFile();
		return dataset;
	}

	public static TransWindowDataset createTransWindowInstanceByName(DatasetParam param, String theta) throws IOException {
		TransWindowDataset dataset = new TransWindowDataset(param, theta);
		PkwiseTokenOrder.run(dataset, Integer.parseInt(param.qlen));
		dataset.loadRecordList(dataset.searchedPath);
		dataset.buildRecordStore();
		dataset.createRuleSet();
		dataset.buildIntQGramStore();
		dataset.addStat();
		dataset.statContainer.finalize();
//		dataset.ruleSet.writeToFile();
		return dataset;
	}

	protected Dataset(DatasetParam param) {
		name = param.getDatasetName();
		this.size = Integer.parseInt(param.size);
		this.lenRatio = Double.parseDouble(param.lenRatio);
		searchedPath = DatasetInfo.getSearchedPath(param.name, param.qlen);
		indexedPath = DatasetInfo.getIndexedPath(param.name);
		rulePath = DatasetInfo.getRulePath(param.name, param.nr);
		outputPath = "output";
		statContainer = new StatContainer();
		statContainer.startWatch(Stat.Time_Prepare_Data);
		statContainer.setStat(Stat.Dataset_Name, name);
		statContainer.setStat(Stat.Dataset_nt, param.size);
		statContainer.setStat(Stat.Dataset_nr, param.nr);
		statContainer.setStat(Stat.Dataset_qlen, param.qlen);
		statContainer.setStat(Stat.Dataset_lr, param.lenRatio);
	}
	
	protected void initTokenIndex() {
		statContainer.startWatch("Time_TokenOrder");
		TokenOrder order = new TokenOrder(this);
		Record.tokenIndex = order.getTokenIndex();
		statContainer.stopWatch("Time_TokenOrder");
	}
	
	protected void createRuleSet() {
		ruleSet = new Ruleset(this);
		Rule.automata = new ACAutomataR(ruleSet.get());
	}
	
	public void addStat() {
		Iterable<Record> searchedList = getSearchedList();
		Iterable<Record> indexedList = getIndexedList();
		statContainer.setStat(Stat.Dataset_numSearched, Integer.toString(getSize(searchedList)));
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(getSize(indexedList)));
		statContainer.setStat(Stat.Dataset_numRule, Integer.toString(ruleSet.size()));
		statContainer.setStat(Stat.Len_SearchedAll, Long.toString(getLengthSum(searchedList)));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(getLengthSum(indexedList)));
		statContainer.stopWatch(Stat.Time_Prepare_Data);
//		try {
//			statContainer.setStat("Size_Recordstore", FileUtils.sizeOfAsBigInteger(new File(RecordStore.path)).toString());
//			}
//		catch ( Exception e ) {}
//		try {
//			statContainer.setStat("Size_IntQGramStore", FileUtils.sizeOfAsBigInteger(new File(IntQGramStore.path)).toString());
//			statContainer.setStat("Num_IntQGrams", Integer.toString(((TransWindowDataset)this).numIntQGrams));
//		}
//		catch ( Exception e ) {}
	}
	
	public abstract Iterable<Record> getSearchedList();

	public abstract Iterable<Record> getIndexedList();
	
	public abstract Record getRecord(int id);

	protected Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : getSearchedList() ) tokenSet.addAll( rec.getTokens() );
		for ( Record rec : getIndexedList() ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}
	
	protected long getLengthSum( Iterable<Record> recordList ) {
		long sum = 0;
		for ( Record rec : recordList ) sum += rec.size();
		return sum;
	}
	
	protected int getSize( Iterable<Record> recordList ) {
		int n = 0;
		for ( @SuppressWarnings("unused") Record rec : recordList ) ++n;
		return n;
	}

	protected final String getPrefixWithLengthRatio(String str) {
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
}

package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
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


	protected Dataset(DatasetParam param) {
		Log.log.trace("Dataset.constructor");
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
		Log.log.trace("Dataset.initTokenIndex()");
		statContainer.startWatch("Time_TokenIndexBuilder");
		Record.tokenIndex = TokenIndexBuilder.build(this);
		statContainer.stopWatch("Time_TokenIndexBuilder");
		Log.log.trace("Dataset.initTokenIndex() finished");
	}
	
	protected abstract void buildRecordStore();
	
	protected void createRuleSet() {
		ruleSet = new Ruleset(this);
		Rule.automata = new ACAutomataR(ruleSet.get());
	}
	
	protected void addStat() {
		Iterable<Record> searchedList = getSearchedList();
		Iterable<Record> indexedList = getIndexedList();
		statContainer.setStat(Stat.Dataset_numSearched, Integer.toString(getSize(searchedList)));
		statContainer.setStat(Stat.Dataset_numIndexed, Integer.toString(getSize(indexedList)));
		statContainer.setStat(Stat.Dataset_numRule, Integer.toString(ruleSet.size()));
		statContainer.setStat(Stat.Len_SearchedAll, Long.toString(getLengthSum(searchedList)));
		statContainer.setStat(Stat.Len_IndexedAll, Long.toString(getLengthSum(indexedList)));
		statContainer.stopWatch(Stat.Time_Prepare_Data);
	}
	
	protected final Iterable<Integer> getDistinctTokens() {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : getSearchedList() ) tokenSet.addAll( rec.getTokens() );
		for ( Record rec : getIndexedList() ) tokenSet.addAll( rec.getTokens() );
		List<Integer> sortedTokenList = tokenSet.stream().sorted().collect(Collectors.toList());
		return sortedTokenList;
	}
	
	protected final long getLengthSum( Iterable<Record> recordList ) {
		long sum = 0;
		for ( Record rec : recordList ) sum += rec.size();
		return sum;
	}
	
	protected final int getSize( Iterable<Record> recordList ) {
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

	public abstract Record getRecord(int id);

	public abstract Iterable<Record> getSearchedList();

	public abstract Iterable<Record> getIndexedList();
	
	public final Iterable<Rule> getRules() {
		return new Iterable<Rule>() {
			
			@Override
			public Iterator<Rule> iterator() {
				return new DiskBasedRuleIterator();
			}
		};
	}

	protected abstract class AbstractDiskBasedIterator<T> implements Iterator<T> {
		
		BufferedReader br;
		Iterator<String> iter;
		int i = 0;
		
		public AbstractDiskBasedIterator(String path) {
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
	
	public final class DiskBasedSearchedRecordIterator extends AbstractDiskBasedIterator<Record> {

		public DiskBasedSearchedRecordIterator() {
			super(searchedPath);
		}

		@Override
		public Record next() {
			String line = iter.next();
			return new Record(i++, line);
		}
	}

	public final class DiskBasedIndexedRecordIterator extends AbstractDiskBasedIterator<Record> {

		public DiskBasedIndexedRecordIterator() {
			super(indexedPath);
		}

		@Override
		public Record next() {
			String line = getPrefixWithLengthRatio(iter.next());
			return new Record(i++, line);
		}

		@Override
		public boolean hasNext() {
			return i < size && iter.hasNext();
		}
	}

	public final class DiskBasedRuleIterator extends AbstractDiskBasedIterator<Rule> {
		
		public DiskBasedRuleIterator() {
			super(rulePath);
		}

		@Override
		public Rule next() {
			return Rule.createRule(iter.next());
		}

	}
}

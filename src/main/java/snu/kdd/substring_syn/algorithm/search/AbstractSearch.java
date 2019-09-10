package snu.kdd.substring_syn.algorithm.search;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Param;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class AbstractSearch {

	protected final String id;
	protected final Param param;
	protected final double theta;
	protected final Set<IntPair> rsltQuerySide;
	protected final Set<IntPair> rsltTextSide;
	protected final StatContainer statContainer;
	
	public AbstractSearch( double theta ) {
		id = FilenameUtils.getBaseName(Log.logpath);

		param = new Param();
		this.theta = theta;
		param.put("theta", Double.toString(theta));

		this.rsltQuerySide = new ObjectOpenHashSet<>();
		this.rsltTextSide = new ObjectOpenHashSet<>();
		statContainer = new StatContainer();
		StatContainer.global = statContainer;
	}
	
	public final void run( Dataset dataset ) {
		statContainer.setAlgorithm(this);
		statContainer.mergeStatContainer(dataset.statContainer);
		statContainer.startWatch(Stat.Time_Total);
		prepareSearch(dataset);
		searchBody(dataset);
		statContainer.stopWatch(Stat.Time_Total);
		putResultIntoStat();
		statContainer.finalizeAndOutput();
		outputResult(dataset);
	}
	
	protected void prepareSearch( Dataset dataset ) {
	}
	
	protected final void searchBody( Dataset dataset ) {
		for ( Record query : dataset.getSearchedList() ) {
			long ts = System.nanoTime();
			searchGivenQuery(query, dataset);
			double searchTime = (System.nanoTime()-ts)/1e6;
			statContainer.addSampleValue("Time_SearchPerQuery", searchTime);
			Log.log.info("search(query=%d, ...)\t%.3f ms", ()->query.getID(), ()->searchTime);
		}
	}
	
	protected final void searchGivenQuery( Record query, Dataset dataset ) {
//		if ( query.getID() != 0 ) return;
		prepareSearchGivenQuery(query);
		statContainer.startWatch(Stat.Time_QSTotal);
		searchQuerySide(query, dataset);
		statContainer.stopWatch(Stat.Time_QSTotal);
		statContainer.startWatch(Stat.Time_TSTotal);
		searchTextSide(query, dataset);
		statContainer.stopWatch(Stat.Time_TSTotal);
	}
	
	protected void prepareSearchGivenQuery( Record query ) {
		query.preprocessAll();
	}
	
	protected final void searchQuerySide( Record query, Dataset dataset ) {
		Iterable<Record> candListQuerySide = getCandRecordListQuerySide(query, dataset);
		int nCand = 0;
		int sumLen = 0;
		for ( Record rec : candListQuerySide ) {
			if ( rsltQuerySide.contains(new IntPair(query.getID(), rec.getID())) ) continue;
			statContainer.addCount(Stat.Len_QS_Retrieved, rec.size());
			searchRecordQuerySide(query, rec);
			++nCand;
			sumLen += rec.size();
		}
		Log.log.debug("SearchQuerySide.nCand=%d", nCand);
		Log.log.debug("SearchQuerySide.sumLen=%d", sumLen);
	}
	
	protected final void searchTextSide( Record query, Dataset dataset ) {
		Iterable<Record> candListTextSide = getCandRecordListTextSide(query, dataset);
		int nCand = 0;
		int sumLen = 0;
		for ( Record rec : candListTextSide ) {
			if ( rsltTextSide.contains(new IntPair(query.getID(), rec.getID())) ) continue;
//			if ( rec.getID() != 29 ) continue;
			statContainer.addCount(Stat.Len_TS_Retrieved, rec.size());
			statContainer.startWatch("Time_TS_searchTextSide.preprocess");
			rec.preprocessAll();
			statContainer.stopWatch("Time_TS_searchTextSide.preprocess");
			searchRecordTextSide(query, rec);
			++nCand;
			sumLen += rec.size();
		}
		Log.log.debug("SearchTextSide.nCand=%d", nCand);
		Log.log.debug("SearchTextSide.sumLen=%d", sumLen);
	}
	
	protected Iterable<Record> getCandRecordListQuerySide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}

	protected Iterable<Record> getCandRecordListTextSide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}
	
	protected final void putResultIntoStat() {
		statContainer.addCount(Stat.Num_Result, countResult());
		statContainer.addCount(Stat.Num_QS_Result, rsltQuerySide.size());
		statContainer.addCount(Stat.Num_TS_Result, rsltTextSide.size());
	}
	
	protected final int countResult() {
		Set<IntPair> rslt = new ObjectOpenHashSet<>(rsltQuerySide);
		rslt.addAll(rsltTextSide);
		return rslt.size();
	}

	protected final void outputResult( Dataset dataset ) {
		try {
			PrintStream ps = new PrintStream(String.format(getOutputPath(dataset), theta));
			getSortedResult(rsltQuerySide).forEach(ip -> ps.println("FROM_QUERY\t"+ip));
			getSortedResult(rsltTextSide).forEach(ip -> ps.println("FROM_TEXT\t"+ip));
			ps.close();
		}
		catch ( FileNotFoundException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected final List<IntPair> getSortedResult( Set<IntPair> rslt ) {
		return rslt.stream().sorted().collect(Collectors.toList());
	}
	
	protected abstract void searchRecordQuerySide( Record query, Record rec );
	
	protected abstract void searchRecordTextSide( Record query, Record rec ); 

	public final String getID() { return id; }
	
	public abstract String getName();

	public abstract String getVersion();
	
	public final StatContainer getStatContainer() {
		return statContainer;
	}
	
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), String.format("%.2f", theta), dataset.name);
	}

	public final String getOutputPath( Dataset dataset ) {
		return "output/"+getOutputName(dataset)+".txt";
	}
	
	public final Param getParam() {
		return param;
	}
}

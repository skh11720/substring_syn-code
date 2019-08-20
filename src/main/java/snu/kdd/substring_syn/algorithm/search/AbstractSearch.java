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
	}
	
	public void run( Dataset dataset ) {
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
	
	protected void searchBody( Dataset dataset ) {
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
		for ( Record rec : candListQuerySide ) {
			statContainer.addCount(Stat.Len_QS_Retrieved, rec.size());
			searchRecordQuerySide(query, rec);
		}
	}
	
	protected final void searchTextSide( Record query, Dataset dataset ) {
		Iterable<Record> candListTextSide = getCandRecordListTextSide(query, dataset);
		for ( Record rec : candListTextSide ) {
//			if ( rec.getID() != 29 ) continue;
			statContainer.addCount(Stat.Len_TS_Retrieved, rec.size());
//			if ( !rsltQuerySide.contains(new IntPair(query.getID(), rec.getID())) ) {
				rec.preprocessAll();
				searchRecordTextSide(query, rec);
//			}
		}
	}
	
	protected Iterable<Record> getCandRecordListQuerySide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}

	protected Iterable<Record> getCandRecordListTextSide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}
	
	protected void putResultIntoStat() {
		statContainer.addCount(Stat.Num_Result, countResult());
		statContainer.addCount(Stat.Num_QS_Result, rsltQuerySide.size());
		statContainer.addCount(Stat.Num_TS_Result, rsltTextSide.size());
	}
	
	protected int countResult() {
		Set<IntPair> rslt = new ObjectOpenHashSet<>(rsltQuerySide);
		rslt.addAll(rsltTextSide);
		return rslt.size();
	}

	protected void outputResult( Dataset dataset ) {
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

	protected List<IntPair> getSortedResult( Set<IntPair> rslt ) {
		return rslt.stream().sorted().collect(Collectors.toList());
	}
	
	protected abstract void searchRecordQuerySide( Record query, Record rec );
	
	protected abstract void searchRecordTextSide( Record query, Record rec ); 

	public String getID() { return id; }
	
	public abstract String getName();

	public abstract String getVersion();
	
	public StatContainer getStatContainer() {
		return statContainer;
	}
	
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), String.format("%.2f", theta), dataset.name);
	}

	public String getOutputPath( Dataset dataset ) {
		return "output/"+getOutputName(dataset)+".txt";
	}
	
	public Param getParam() {
		return param;
	}
}

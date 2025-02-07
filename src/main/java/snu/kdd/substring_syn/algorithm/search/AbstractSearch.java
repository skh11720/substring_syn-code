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
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
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
	
	protected Dataset dataset;
	
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
		Log.log.trace("AbstractSearch.run()");
		this.dataset = dataset;
		statContainer.setAlgorithm(this);
		statContainer.startWatch(Stat.Time_Total);
		prepareSearch(dataset);
		searchBody(dataset);
		statContainer.stopWatch(Stat.Time_Total);
		putResultIntoStat();
		dataset.addStat();
		dataset.statContainer.finalize();
		statContainer.mergeStatContainer(dataset.statContainer);
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
			statContainer.addSampleValue(Stat.Time_SearchPerQuery, searchTime);
			Log.log.info("search(query=%d, ...)\t%.3f ms", ()->query.getIdx(), ()->searchTime);
		}
	}
	
	protected final void searchGivenQuery( Record query, Dataset dataset ) {
		prepareSearchGivenQuery(query);
		statContainer.startWatch(Stat.Time_QS_Total);
		searchQuerySide(query, dataset);
		statContainer.stopWatch(Stat.Time_QS_Total);
		statContainer.startWatch(Stat.Time_TS_Total);
		searchTextSide(query, dataset);
		statContainer.stopWatch(Stat.Time_TS_Total);
	}
	
	protected void prepareSearchGivenQuery( Record query ) {
		query.preprocessAll();
	}
	
	protected final void searchQuerySide( Record query, Dataset dataset ) {
		Iterable<TransformableRecordInterface> candListQuerySide = getCandRecordListQuerySide(query, dataset);
		for ( TransformableRecordInterface rec : candListQuerySide ) {
			if (rsltQuerySideContains(query, rec)) continue;
			statContainer.addCount(Stat.Num_QS_Retrieved, 1);
			statContainer.addCount(Stat.Len_QS_Retrieved, rec.size());
			searchRecordQuerySide(query, rec);
		}
	}
	
	protected final void searchTextSide( Record query, Dataset dataset ) {
		Iterable<TransformableRecordInterface> candListTextSide = getCandRecordListTextSide(query, dataset);
		for ( TransformableRecordInterface rec : candListTextSide ) {
			if (rsltTextSideContains(query, rec)) continue;
			statContainer.addCount(Stat.Num_TS_Retrieved, 1);
			statContainer.addCount(Stat.Len_TS_Retrieved, rec.size());
			searchRecordTextSide(query, rec);
		}
	}
	
	protected final boolean rsltQuerySideContains(Record query, RecordInterface rec) {
		if (dataset.isDocInput()) return rsltQuerySide.contains(new IntPair(query.getID(), dataset.getRid2idpairMap().get(rec.getIdx()).i1));
		else return rsltQuerySide.contains(new IntPair(query.getID(), rec.getID()));
	}

	protected final boolean rsltTextSideContains(Record query, RecordInterface rec) {
		if (dataset.isDocInput()) return rsltTextSide.contains(new IntPair(query.getID(), dataset.getRid2idpairMap().get(rec.getIdx()).i1));
		else return rsltTextSide.contains(new IntPair(query.getID(), rec.getID()));
	}
	
	protected Iterable<TransformableRecordInterface> getCandRecordListQuerySide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}

	protected Iterable<TransformableRecordInterface> getCandRecordListTextSide(Record query, Dataset dataset) {
		return dataset.getIndexedList();
	}
	
	protected final void addResultQuerySide(Record query, RecordInterface rec) {
		if (dataset.isDocInput()) rsltQuerySide.add(new IntPair(query.getID(), dataset.getRid2idpairMap().get(rec.getIdx()).i1));
		else rsltQuerySide.add(new IntPair(query.getID(), rec.getID()));
	}

	protected void addResultTextSide(Record query, RecordInterface rec) {
		if (dataset.isDocInput()) rsltTextSide.add(new IntPair(query.getID(), dataset.getRid2idpairMap().get(rec.getIdx()).i1));
		else rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
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
	
	protected abstract void searchRecordQuerySide( Record query, RecordInterface rec );
	
	protected abstract void searchRecordTextSide( Record query, TransformableRecordInterface rec ); 

	public final String getID() { return id; }
	
	public abstract String getName();

	public abstract String getVersion();
	
	public final StatContainer getStatContainer() {
		return statContainer;
	}
	
	public final String getStat(String key) {
		return statContainer.getStat(key);
	}
	
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), String.format("%.2f", theta), dataset.name);
	}

	public final String getOutputPath( Dataset dataset ) {
		return "output/"+getOutputName(dataset)+".txt";
	}
	
	public final Set<IntPair> getResultTextSide() {
		return rsltTextSide;
	}
	
	public final Param getParam() {
		return param;
	}
}
